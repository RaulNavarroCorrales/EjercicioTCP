package ejercicio.servidor;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainServidor {
    private static final int PORT = 1235; // Puerto en el que se ejecutará el servidor
    private static final List<String> historialMensajes = new ArrayList<>(); // Historial de mensajes del chat
    private static final Set<String> nicknames = new HashSet<>(); // Lista de nicknames únicos de los usuarios conectados
    private static final List<ObjectOutputStream> clientes = new ArrayList<>(); // Lista de flujos de salida para enviar mensajes a los clientes conectados
    private static JTextArea jTextArea; // Área de texto para mostrar información en la ventana del servidor

    public static void main(String[] args) {
        //Creo el JFrame para ver el servidor:
        JFrame frame = new JFrame("Servidor del Chat");
        frame.setSize(400, 500);
        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        frame.add(new JScrollPane(jTextArea));
        frame.setVisible(true);

        //Inicio del servidor:
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            jTextArea.append("Servidor en línea en el puerto " + PORT + "\n");
            while (true) {
                Socket socket = serverSocket.accept();
                //Envío de la petición del cliente:
                manejarCliente(socket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Manejo del inicio se sesión de los usuarios, asi como la carga del historial de mensajes
     * @param socket
     * Envío del cliente al servidor
     */
    private static void manejarCliente(Socket socket) {
        Thread manejoClientes = new Thread(() -> {
            try {
                //Envío de datos al cliente:
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                //Recepción de datos del cliente:
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                String usuario;

                while (true) {
                    //Obtengo el usuario del cliente:
                    usuario = (String) in.readObject();
                    //Permitir varios inicios de sesión simultáneos:
                    synchronized (nicknames) {
                        if (nicknames.contains(usuario)) {
                            out.writeObject("[ERROR] El usuario '" + usuario + "' ya está en uso.");
                        } else {
                            //Añadir el cliente a la lista nicknames:
                            nicknames.add(usuario);
                            //Añadir al cliente a la lista de clientes conectados para enviar los mensajes:
                            clientes.add(out);
                            //Indicar en el servidor que el cliente se ha conectado:
                            jTextArea.append("Usuario conectado: " + usuario + "\n");
                            //Cargar el historial de mensajes al cliente conectado:
                            for (String msg : historialMensajes) {
                                out.writeObject(msg);
                            }
                            out.writeObject("[INFO] Bienvenido, " + usuario);
                            break;
                        }
                    }
                }
                String mensaje;
                while ((mensaje = (String) in.readObject()) != null) {
                    //Cargar los mensajes al historial e indicar en el servidor que se ha recibido el mensaje y su contenido:
                    historialMensajes.add(mensaje);
                    jTextArea.append("Mensaje recibido: " + mensaje + "\n");
                    enviarMensajeATodos(mensaje);
                }
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        manejoClientes.start();
    }

    /**
     * Reenvío del mensaje a todos los clientes conectados al servidor:
     * @param mensaje
     * Cadena de texto a enviar al chat de los clientes conectados
     */
    private static void enviarMensajeATodos(String mensaje) {
        for (ObjectOutputStream cliente : clientes) {
            try {
                cliente.writeObject(mensaje);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}