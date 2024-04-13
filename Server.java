/**
 * JAVA SOCKET SERVER
 *
 * @author Aastha Bhatt
 * @author Matthew Dibbern
 * @author Vincent Gruse
 * @author Leo Tangban
 * @author Emmanuel Taylor
 *
 * @description
 *    This class represents a Java UDP Socket Server that waits for request from a Client containing
 *    a web server and timer value. A new thread is created to handle client communication and processing.
 *    The server sends the response from the web server in packets of size 1000 and waits for an ACK from
 *    the client.
 *
 * @packages
 *    Java IO (BufferedReader, ByteArrayOutputStream, DataOutputStream, IOException, InputStreamReader)
 *    Java Net (DatagramPacket, DatagramSocket, HttpURLConnection, InetAddress, SocketTimeoutException, URL)
 *    Java NIO Charset (StandardCharsets)
 *    Java Utilities (Arrays, Scanner)
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Server {
    public static final int SERVER_PORT = 11111;
    public static final int ACK_PORT = 11112; // Port for receiving acknowledgments from the client
    public static final int PACKET_SIZE = 1000; // Maximum size of data packet
    public static final int SOCKET_TIMEOUT = 3000; // Socket timeout in milliseconds

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("\n==================================================================================");
            System.out.println("=========================JAVA SOCKET APPLICATION - SERVER=========================");
            System.out.println("==================================================================================\n");

            System.out.println("Listening on Port " + SERVER_PORT + "...\n");

            Scanner scanner = new Scanner(System.in);
            boolean isRunning = true;
            while (isRunning) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String clientRequest = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Extract web server name and timer value from client request
                String[] parts = clientRequest.split(",");
                String webServerName = parts[0];
                int timerValue = Integer.parseInt(parts[1]);
                System.out.println("Received Client Request: " + webServerName + ", Timer: " + timerValue + " seconds\n");

                // Start a separate thread to handle client communication
                Thread clientHandlerThread = new Thread(new ClientHandler(webServerName, timerValue, receivePacket.getAddress(), receivePacket.getPort(), serverSocket));
                clientHandlerThread.start();

                // Check for user input to stop the server
                System.out.print("Enter 'stop' to shutdown the server: ");
                String userInput = scanner.nextLine();
                if ("stop".equalsIgnoreCase(userInput)) {
                    isRunning = false;
                }
            }
        } catch (IOException e) {
            System.err.println("Error occurred in the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final String webServerName;
    private final int timerValue;
    private final InetAddress clientAddress;
    private final int clientPort;
    private final DatagramSocket serverSocket;

    public ClientHandler(String webServerName, int timerValue, InetAddress clientAddress, int clientPort, DatagramSocket serverSocket) {
        this.webServerName = webServerName;
        this.timerValue = timerValue;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            sendDataToClient();
        } catch (IOException e) {
            System.err.println("Error occurred while sending data to client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send data to the client and wait for acknowledgment
    private void sendDataToClient() throws IOException {
        try {
            long startTime = System.currentTimeMillis();
            String responseData = getResponseFromWebServer();
            sendResponseInPackets(responseData);
            waitForAck();
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println(elapsedTime < timerValue * 1000L ? "DONE\n" : "RESENT\n");
        } catch (SocketTimeoutException e) {
            System.err.println("ACK not received from Client in time. Resending...");
            sendDataToClient(); // Attempt resending
        }
    }

    // Retrieve response data from the web server
    private String getResponseFromWebServer() throws IOException {
        URL url = new URL("https://" + webServerName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder responseData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseData.append(line);
            }
        } finally {
            connection.disconnect();
        }

        return responseData.toString();
    }

    // Send response data to the client in packets
    private void sendResponseInPackets(String responseData) throws IOException {
        byte[] responseDataBytes = responseData.getBytes(StandardCharsets.UTF_8);
        int totalPackets = (int) Math.ceil((double) responseDataBytes.length / Server.PACKET_SIZE);

        for (int i = 0; i < totalPackets; i++) {
            int payloadLength = Math.min(Server.PACKET_SIZE, responseDataBytes.length - i * Server.PACKET_SIZE);
            byte[] payload = Arrays.copyOfRange(responseDataBytes, i * Server.PACKET_SIZE, i * Server.PACKET_SIZE + payloadLength);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
                // Prepend packet metadata (packet index, total packets, payload length)
                dataOutputStream.writeBytes(i + "," + totalPackets + "," + payloadLength + "xxx");
                dataOutputStream.write(payload);
                byte[] packetData = outputStream.toByteArray();
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientAddress, clientPort);
                serverSocket.send(packet);
            }
        }
    }

    // Wait for acknowledgment from the client
    private void waitForAck() throws IOException {
        try (DatagramSocket ackSocket = new DatagramSocket(Server.ACK_PORT)) {
            ackSocket.setSoTimeout(Server.SOCKET_TIMEOUT);
            byte[] receiveData = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(receiveData, receiveData.length);
            ackSocket.receive(ackPacket);
            System.out.println("ACK received from Client.\n");
        }
    }
}
