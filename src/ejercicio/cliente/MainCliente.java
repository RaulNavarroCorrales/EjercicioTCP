package ejercicio.cliente;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class MainCliente {
    private static final int PORT = 1235; // Puerto para la conexión con el servidor
    private static ObjectOutputStream out; // Flujo de salida para enviar objetos al servidor
    private static ObjectInputStream in; // Flujo de entrada para recibir objetos del servidor
    private static String nickname; // Nickname ingresado por el usuario
    private static JTextArea chatArea; // Área de texto para mostrar el chat
    private static JTextField mensajeField; // Área de texto para escribir mensajes

    public static void main(String[] args) {
        //Configuramos la conexión del cliente al servidor:
        try {
            Socket socketCliente = new Socket("localhost", PORT);
            //Envío de datos al servidor:
            out = new ObjectOutputStream(socketCliente.getOutputStream());
            //Recepción de datos del servidor:
            in = new ObjectInputStream(socketCliente.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Creación del JFrame llamado Chat para poder usar el programa de una forma más estética:
        JFrame frame = new JFrame("Chat");
        frame.setSize(400, 500);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea));
        //Área de escritura del mensaje:
        mensajeField = new JTextField();
        //Botón de envío:
        JButton enviarButton = new JButton("Enviar");
        //Creación del panel inferior para poner el área de escritura y el botón de envío:
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(mensajeField, BorderLayout.CENTER);
        inputPanel.add(enviarButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);
        //Posibilidad de enviar el mensaje haciendo clic en el botón de enviar o desde la misma área de escritura pulsando intro:
        enviarButton.addActionListener(e -> enviarMensaje());
        mensajeField.addActionListener(e -> enviarMensaje());

        //Mostrar el JFrame del Chat para el usuario:
        frame.setVisible(true);

        solicitarUsuario();
        iniciarEscuchaMensajes();
    }

    /**
     * Solicitud de inicio de sesión de un usuario, se solicita mediante un mensaje emergente al iniciar el chat.
     * Se solicitarán nicknames hasta ingresar uno válido, no pueden estar repetidos, ya que en este caso, el servidor
     * contestara la solicitud con un mensaje de ERROR y se volverá a pedir ingresar el nickname.
     */
    private static void solicitarUsuario() {
        boolean usuarioAceptado = false;
        while (!usuarioAceptado) {
            //Pedir mediante un mensaje emergente el nickname:
            nickname = JOptionPane.showInputDialog("Introduce tu nickname:");
            //Si el usuario ingresado está vacío o es nulo se volvera a pedir sin haber mandado la solicitud al servidor:
            if (nickname == null || nickname.trim().isEmpty()){ continue;}

            try {
                //Enviar el nickname al servidor:
                out.writeObject(nickname);
                //Enviar al servidor;
                out.flush();
                String respuestaServidor = (String) in.readObject();
                //Clasificar la respuesta del servidor:
                if (respuestaServidor.startsWith("[ERROR]")) {
                    JOptionPane.showMessageDialog(null, respuestaServidor, "ERROR", JOptionPane.ERROR_MESSAGE);
                } else {
                    //Escribir la respuesta del servidor (bienvenida) en el chat:
                    chatArea.append(respuestaServidor + "\n");
                    //Salir del bucle:
                    usuarioAceptado = true;
                }
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Después de iniciar sesión empieza la escucha de mensajes desde el servidor, por lo que mediante un hilo podremos
     * ver todos los mensajes sin errores ni interrupciones.
     */
    private static void iniciarEscuchaMensajes() {
        Thread escuchaMensajes = new Thread(() -> {
            try {
                while (true) {
                    //Obtener los mensajes desde el servidor:
                    String mensajeRecibido = (String) in.readObject();
                    //Añadir el mensaje recibido al Chat:
                    chatArea.append(mensajeRecibido + "\n");
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        escuchaMensajes.start();
    }

    /**
     * Envío de mensajes al servidor el cual más tarde enviará el mensaje a todos los demás usuarios en línea.
     */
    private static void enviarMensaje() {
        //Obtener el mensaje del área de escritura:
        String mensaje = mensajeField.getText().trim();
        if (!mensaje.isEmpty()) {
            try {
                //Enviar el mensaje con el formato = nickname: mensaje:
                out.writeObject(nickname + ": " + mensaje);
                //Enviar al servidor:
                out.flush();
                //Limpiar el área de escritura:
                mensajeField.setText("");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
