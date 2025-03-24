package core.network;

public class Peer {

    private final String address;
    private final int port;

    public Peer(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Peer{" + "address='" + address + '\'' + ", port=" + port + '}';
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Peer peer && peer.getAddress()
                .equals(address) && peer.getPort() == port;
    }

    @Override
    public int hashCode() {
        return address.hashCode() + Integer.hashCode(port);
    }
}
