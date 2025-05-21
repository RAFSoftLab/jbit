package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import core.network.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class HandshakeClient {

    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private final Selector se;

    public HandshakeClient(Selector selector) {
        this.se = selector;
    }

    public List<PeerConnection> handshake(List<Peer> peers, TorrentFile torrentFile) {
        try {
            Selector selector = Selector.open();
            List<PeerConnection> connections = new ArrayList<>();
            int countSent = 0;
            int countRead = 0;

            int countC = 0;


            for (Peer peer : peers) {
                try {

                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress(peer.getAddress(), peer.getPort()));
                    countC++;

                    socketChannel.register(selector, SelectionKey.OP_CONNECT, peer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Attempting to connect to: " + countC + " peers");

            long startTime = System.currentTimeMillis();
            //ByteBuffer buffer = handshakeMessage.create().flip();
            int iterations = 0;
            int connected = 0;
            final Handshake handshakeMessage = new Handshake(torrentFile);
            try {
                while ((System.currentTimeMillis() - startTime) < CONNECTION_TIMEOUT_MS) {
                    iterations++;
                    if (selector.select(100) == 0) continue;


                    Iterator<SelectionKey> iterator = selector.selectedKeys()
                            .iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isConnectable()) {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            try {
                                if (socketChannel.finishConnect()) {
                                    key.interestOps(SelectionKey.OP_WRITE);
                                    ByteBuffer writeBuffer = handshakeMessage.create()
                                            .flip();

                                    while (writeBuffer.hasRemaining()) {
                                        socketChannel.write(writeBuffer);
                                    }
                                    countSent++;
                                    key.interestOps(SelectionKey.OP_READ);
                                    connected++;

                                } else {
                                    key.cancel();
                                    socketChannel.close();
                                    System.out.println("Falied to connect to: " + socketChannel.socket()
                                            .getInetAddress()
                                            .getHostAddress() + ":" + socketChannel.socket()
                                            .getPort());

                                }
                            } catch (IOException e) {
                                key.cancel();
                                try {
                                    socketChannel.close();
                                } catch (IOException ignored) {
                                }
                            }
                        } else if (key.isReadable()) {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            try {
                                ByteBuffer readBuffer = ByteBuffer.allocate(68);
                                int read = socketChannel.read(readBuffer);
                                if (read == -1) {
                                    key.cancel();
                                    socketChannel.close();
                                }
                                countRead++;
                                if (handshakeMessage.parse(readBuffer)) {
                                    PeerConnection peerConnection =
                                            new PeerConnection((Peer) key.attachment(), torrentFile, socketChannel);
                                    connections.add(peerConnection);
                                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    SelectableChannel channel = key.channel();
                                    key.cancel();
                                    channel.register(se, SelectionKey.OP_READ | SelectionKey.OP_WRITE, peerConnection);
                                } else {
                                    key.cancel();
                                    socketChannel.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                key.cancel();
                                socketChannel.close();
                            }
                        }

                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                selector.close();
                this.se.wakeup();
                System.out.println("Connections established: " + connected + " Iterations: " + iterations);
                System.out.println("Handshake sent: " + countSent + " Handshake read: " + countRead);
            }

            return connections;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}






