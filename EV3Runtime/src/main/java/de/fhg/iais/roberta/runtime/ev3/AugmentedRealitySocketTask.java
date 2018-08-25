package de.fhg.iais.roberta.runtime.ev3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class AugmentedRealitySocketTask implements Callable<Void> {
    private String hostName;
    private int portNumber;

    private Selector selector;
    private SocketChannel client;
    private ByteBuffer readBuffer = ByteBuffer.allocate(Float.SIZE / Byte.SIZE);

    private volatile float distance = Float.MAX_VALUE;

    public AugmentedRealitySocketTask(String hostName, int portNumber) {
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    @Override
    public Void call() throws IOException {
        try {
            this.selector = Selector.open();
            this.client = SocketChannel.open();
            this.client.connect(new InetSocketAddress(this.hostName, this.portNumber));
            this.client.configureBlocking(false);
            this.client.register(this.selector, SelectionKey.OP_WRITE);

            while (!Thread.currentThread().isInterrupted()) {
                this.selector.select(5000L);
                Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (key.isReadable()) {
                        this.read(key);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }

                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } finally {
            for (SelectionKey key : this.selector.keys()) {
                key.channel().close();
                key.cancel();
            }
            this.selector.close();
            this.client.close();
        }
        return null;
    }

    private void read(SelectionKey key) throws IOException, ClosedChannelException {
        SocketChannel client = (SocketChannel) key.channel();
        this.readBuffer.clear();

        client.read(this.readBuffer);
        this.readBuffer.flip();

        this.distance = this.readBuffer.getFloat();

        client.register(this.selector, SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException, ClosedChannelException {
        SocketChannel client = (SocketChannel) key.channel();
        client.write(ByteBuffer.wrap("alive".getBytes()));

        client.register(this.selector, SelectionKey.OP_READ);
    }

    public float getDistance() {
        return this.distance;
    }
}
