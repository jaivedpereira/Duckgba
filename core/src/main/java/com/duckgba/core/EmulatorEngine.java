package com.duckgba.core;

import eu.rekawek.coffeegb.core.Gameboy;
import eu.rekawek.coffeegb.core.GameboyType;
import eu.rekawek.coffeegb.core.events.EventBus;
import eu.rekawek.coffeegb.core.events.EventBusImpl;
import eu.rekawek.coffeegb.core.gpu.Display;
import eu.rekawek.coffeegb.core.joypad.Button;
import eu.rekawek.coffeegb.core.joypad.ButtonPressEvent;
import eu.rekawek.coffeegb.core.joypad.ButtonReleaseEvent;
import eu.rekawek.coffeegb.core.memento.Memento;
import eu.rekawek.coffeegb.core.memory.cart.Rom;
import eu.rekawek.coffeegb.core.serial.SerialEndpoint;
import eu.rekawek.coffeegb.core.sound.Sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thin Android-friendly wrapper around the embedded coffee-gb core. Hides all
 * the threading, event wiring and bitmap conversion details from the UI layer.
 *
 * Usage:
 * <pre>
 *   EmulatorEngine engine = new EmulatorEngine();
 *   engine.setFrameListener(pixels -> updateBitmap(pixels));
 *   engine.setAudioListener(samples -> audioTrack.write(samples, ...));
 *   engine.loadRom(romFile, batterySaveFile, EmulatorEngine.Options.DEFAULT);
 *   engine.start();
 *   ...
 *   engine.pressButton(EmulatorEngine.GbButton.A);
 *   ...
 *   engine.stop();
 * </pre>
 *
 * The instance is single-shot: call {@link #close()} when done.
 */
public class EmulatorEngine {

    public static final int SCREEN_WIDTH = Display.DISPLAY_WIDTH;     // 160
    public static final int SCREEN_HEIGHT = Display.DISPLAY_HEIGHT;   // 144
    public static final int SCREEN_PIXELS = SCREEN_WIDTH * SCREEN_HEIGHT;

    /** Audio output sample rate, in Hz. The core emits stereo samples here. */
    public static final int AUDIO_SAMPLE_RATE = 22050;

    public enum GbButton {
        UP(Button.UP), DOWN(Button.DOWN), LEFT(Button.LEFT), RIGHT(Button.RIGHT),
        A(Button.A), B(Button.B), START(Button.START), SELECT(Button.SELECT);

        final Button core;
        GbButton(Button core) { this.core = core; }
    }

    /** Pluggable listener invoked whenever a fresh frame is ready (160x144 ARGB). */
    public interface FrameListener {
        void onFrame(int[] argb);
    }

    /** Pluggable listener invoked with raw stereo audio samples produced by the core. */
    public interface AudioListener {
        void onSamples(int[] interleavedStereo);
    }

    public static final class Options {
        public final int[] dmgPalette;
        public final boolean forceDmg;
        public final boolean skipBios;
        public final boolean enableAudio;
        public final boolean enableBatterySave;

        public Options(int[] dmgPalette, boolean forceDmg, boolean skipBios,
                       boolean enableAudio, boolean enableBatterySave) {
            this.dmgPalette = dmgPalette.clone();
            this.forceDmg = forceDmg;
            this.skipBios = skipBios;
            this.enableAudio = enableAudio;
            this.enableBatterySave = enableBatterySave;
        }

        public static Options DEFAULT = new Options(
                Display.DmgFrameReadyEvent.COLORS,
                false, true, true, true);
    }

    private final Object lock = new Object();
    private Gameboy gameboy;
    private Thread thread;
    private EventBus eventBus;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean turbo = new AtomicBoolean(false);
    private FrameListener frameListener;
    private AudioListener audioListener;
    private final int[] frameBuffer = new int[SCREEN_PIXELS];
    private volatile int[] dmgPalette = Display.DmgFrameReadyEvent.COLORS.clone();
    private volatile File batteryFile;

    /** Set the listener notified of each new frame (called from the emulation thread). */
    public void setFrameListener(FrameListener listener) {
        this.frameListener = listener;
    }

    /** Set the listener notified of each audio sample bundle (called from the emulation thread). */
    public void setAudioListener(AudioListener listener) {
        this.audioListener = listener;
    }

    public void setDmgPalette(int[] palette) {
        if (palette == null || palette.length != 4) return;
        this.dmgPalette = palette.clone();
    }

    public void loadRom(File romFile, File batteryFile, Options options) throws IOException {
        synchronized (lock) {
            if (gameboy != null) {
                throw new IllegalStateException("Engine already has a ROM loaded");
            }
            Rom rom = new Rom(romFile);
            this.batteryFile = options.enableBatterySave ? batteryFile : null;

            Gameboy.GameboyConfiguration cfg = new Gameboy.GameboyConfiguration(rom)
                    .setBootstrapMode(options.skipBios
                            ? Gameboy.BootstrapMode.SKIP
                            : Gameboy.BootstrapMode.NORMAL)
                    .setSupportBatterySave(options.enableBatterySave)
                    .setDisplaySgbBorder(false);

            if (options.forceDmg) {
                cfg.setGameboyType(GameboyType.DMG);
            } else if (rom.getGameboyColorFlag() == Rom.GameboyColorFlag.NON_CGB) {
                cfg.setGameboyType(GameboyType.DMG);
            } else {
                cfg.setGameboyType(GameboyType.CGB);
            }

            this.dmgPalette = options.dmgPalette.clone();

            Gameboy gb = cfg.build();
            EventBus bus = new EventBusImpl(null, "duckgba", false);
            bus.register(event -> deliverDmgFrame(event), Display.DmgFrameReadyEvent.class);
            bus.register(event -> deliverGbcFrame(event), Display.GbcFrameReadyEvent.class);
            bus.register(event -> deliverAudio(event), Sound.SoundSampleEvent.class);

            gb.init(bus, SerialEndpoint.NULL_ENDPOINT, null);

            // If a previous battery save (memento) exists, replay it onto the fresh
            // gameboy so the player picks up where they left off.
            if (options.enableBatterySave && batteryFile != null && batteryFile.exists() && batteryFile.length() > 0) {
                tryRestoreMemento(gb, batteryFile);
            }

            this.gameboy = gb;
            this.eventBus = bus;
        }
    }

    @SuppressWarnings("unchecked")
    private static void tryRestoreMemento(Gameboy gb, File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Memento<Gameboy> mem = (Memento<Gameboy>) in.readObject();
            gb.restoreFromMemento(mem);
        } catch (Throwable ignored) {
            // Corrupt or incompatible save; fall back to a fresh boot.
        }
    }

    public void start() {
        synchronized (lock) {
            if (gameboy == null) throw new IllegalStateException("No ROM loaded");
            if (thread != null && thread.isAlive()) return;
            running.set(true);
            thread = new Thread(this::runLoop, "Duckgba-Emu");
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            thread.start();
        }
    }

    public void stop() {
        Thread t;
        synchronized (lock) {
            running.set(false);
            if (gameboy != null) gameboy.stop();
            t = thread;
            thread = null;
        }
        if (t != null) {
            try { t.join(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    public void pause() {
        synchronized (lock) {
            if (gameboy != null && !gameboy.isPaused()) gameboy.pause();
        }
    }

    public void resume() {
        synchronized (lock) {
            if (gameboy != null && gameboy.isPaused()) gameboy.resume();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setTurbo(boolean enabled) {
        turbo.set(enabled);
    }

    public void pressButton(GbButton button) {
        EventBus bus = this.eventBus;
        if (bus != null) bus.post(new ButtonPressEvent(button.core));
    }

    public void releaseButton(GbButton button) {
        EventBus bus = this.eventBus;
        if (bus != null) bus.post(new ButtonReleaseEvent(button.core));
    }

    /**
     * Persist a save state that can later be restored with {@link #loadState(File)}.
     */
    public boolean saveState(File destination) {
        Gameboy gb;
        boolean wasPaused;
        synchronized (lock) {
            if (gameboy == null) return false;
            gb = gameboy;
            wasPaused = gb.isPaused();
            if (!wasPaused) gb.pause();
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(destination))) {
            out.writeObject(gb.saveToMemento());
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (!wasPaused) gb.resume();
        }
    }

    @SuppressWarnings("unchecked")
    public boolean loadState(File source) {
        Gameboy gb;
        boolean wasPaused;
        synchronized (lock) {
            if (gameboy == null) return false;
            gb = gameboy;
            wasPaused = gb.isPaused();
            if (!wasPaused) gb.pause();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            Memento<Gameboy> mem = (Memento<Gameboy>) in.readObject();
            gb.restoreFromMemento(mem);
            return true;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return false;
        } finally {
            if (!wasPaused) gb.resume();
        }
    }

    public void close() {
        stop();
        synchronized (lock) {
            if (gameboy != null) {
                gameboy.close();
                gameboy = null;
            }
            if (eventBus != null) {
                eventBus.close();
                eventBus = null;
            }
        }
    }

    private void runLoop() {
        Gameboy gb;
        synchronized (lock) {
            gb = gameboy;
        }
        if (gb == null) return;
        try {
            // Mirror Gameboy.run() but observe the turbo flag and the running flag.
            while (running.get()) {
                if (turbo.get()) {
                    // Run a few extra frames worth of ticks per iteration.
                    int budget = Gameboy.TICKS_PER_FRAME * 2;
                    while (budget-- > 0 && running.get()) {
                        if (gb.isPaused()) { sleepQuiet(2); continue; }
                        gb.tick();
                    }
                } else {
                    if (gb.isPaused()) { sleepQuiet(5); continue; }
                    gb.tick();
                }
            }
        } catch (Throwable t) {
            // Engine threads must never crash the host process silently; rethrow on dev
            // builds is tempting but the safer behaviour is to stop and let the UI react.
            running.set(false);
        }
    }

    private void deliverDmgFrame(Display.DmgFrameReadyEvent event) {
        FrameListener listener = frameListener;
        if (listener == null) return;
        event.toRgb(frameBuffer, dmgPalette);
        // Convert RGB into ARGB (opaque)
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = 0xFF000000 | (frameBuffer[i] & 0x00FFFFFF);
        }
        listener.onFrame(frameBuffer);
    }

    private void deliverGbcFrame(Display.GbcFrameReadyEvent event) {
        FrameListener listener = frameListener;
        if (listener == null) return;
        event.toRgb(frameBuffer);
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = 0xFF000000 | (frameBuffer[i] & 0x00FFFFFF);
        }
        listener.onFrame(frameBuffer);
    }

    private void deliverAudio(Sound.SoundSampleEvent event) {
        AudioListener listener = audioListener;
        if (listener == null) return;
        listener.onSamples(event.buffer());
    }

    /**
     * Snapshot the current emulator state (CPU registers, RAM, cartridge battery RAM)
     * to the on-disk save file associated with the current ROM. Best-effort, used
     * to provide automatic save-on-pause behaviour for cartridges without explicit
     * battery save events.
     */
    public void flushBatteryToDisk() {
        File target = batteryFile;
        if (target == null) return;
        saveState(target);
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
