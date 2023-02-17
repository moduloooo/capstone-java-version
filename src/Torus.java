import static java.lang.Math.*;
import org.joml.*;

public class Torus {
    private int numVertices, numIndices, precision;
    private int[] indices;
    private Vector3f[] vertices;
    private Vector2f[] texCoords;
    private Vector3f[] normals;

    public Torus (int p) {
        precision = p;
        initTorus();
    }

    private void initTorus() {

    }

    public int getNumIndices() {
        return numIndices;
    }

    public int getNumVertices() {
        return numVertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public Vector3f[] getVertices() {
        return vertices;
    }

    public Vector2f[] getTexCoords() {
        return texCoords;
    }

    public Vector3f[] getNormals() {
        return normals;
    }
    
}
