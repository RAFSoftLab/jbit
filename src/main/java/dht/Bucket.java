package dht;

public class Bucket {

    private Node[] nodes;

    private int min;
    private int max;

    public Bucket(int min, int max){
        this.min = min;
        this.max = max;
        this.nodes = new Node[8];
    }

    public int getSize(){
        return nodes.length;
    }

}
