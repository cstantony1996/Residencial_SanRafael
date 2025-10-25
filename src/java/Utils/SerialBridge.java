package Utils;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SerialBridge {

    private static final String PORT_NAME = "COM3";  
    private static final int BAUD = 9600;

    private static SerialPort port;
    private static volatile boolean opening = false;
    private static volatile String lastError = null;

    private SerialBridge() {
    }

    public static String getPortName() {
        return PORT_NAME;
    }

    public static String getLastError() {
        return lastError;
    }

    private static synchronized boolean ensureOpen() {
        try {
            if (port != null && port.isOpen()) {
                return true;
            }
            if (opening) {
                return (port != null && port.isOpen());
            }
            opening = true;
            lastError = null;

            port = SerialPort.getCommPort(PORT_NAME);
            port.setComPortParameters(BAUD, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

            if (!port.openPort()) {
                lastError = "No se pudo abrir el puerto serial: " + PORT_NAME;
                port = null;
                return false;
            }
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
            }
            return true;
        } catch (Exception e) {
            lastError = "Error abriendo puerto " + PORT_NAME + ": " + e.getMessage();
            safeClose();
            return false;
        } finally {
            opening = false;
        }
    }

    public static synchronized boolean writeLine(String line) {
        if (!ensureOpen()) {
            return false;
        }
        if (port == null || !port.isOpen()) {
            lastError = "Puerto no abierto.";
            return false;
        }
        try {
            String withNL = line.endsWith("\n") ? line : (line + "\n");
            byte[] data = withNL.getBytes(StandardCharsets.UTF_8);
            port.getOutputStream().write(data);
            port.getOutputStream().flush();
            return true;
        } catch (IOException e) {
            lastError = "Error escribiendo al puerto: " + e.getMessage();
            safeClose();
            return false;
        }
    }

    public static boolean pulseEntrada() {
        return writeLine("PULSE;E");
    }

    public static boolean pulseSalida() {
        return writeLine("PULSE;S");
    }

    private static void safeClose() {
        try {
            if (port != null && port.isOpen()) {
                port.closePort();
            }
        } catch (Exception ignored) {
        }
        port = null;
    }
}
