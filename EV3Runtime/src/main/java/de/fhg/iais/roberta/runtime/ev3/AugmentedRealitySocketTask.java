package de.fhg.iais.roberta.runtime.ev3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AugmentedRealitySocketTask implements Callable<Void> {
    private String hostName;
    private int portNumber;

    private volatile float distance;

    public AugmentedRealitySocketTask(String hostName, int portNumber) {
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    @Override
    public Void call() throws IOException {
        Socket socket = new Socket(this.hostName, this.portNumber);
        final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String fromServer;

        // Start heartbeat thread
        Future future = Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        out.println("alive");
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        while ((fromServer = in.readLine()) != null) {
            this.distance = Float.parseFloat(fromServer);
        }
        future.cancel(true);
        socket.close();
        out.close();
        in.close();
        return null;
    }

    public float getDistance() {
        return this.distance;
    }
}
