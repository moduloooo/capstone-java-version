import static java.lang.Math.*;
import org.joml.*;

//procedurally generated sphere -- so that I don't have to calculate every single vertex position by hand
public class Sphere {
    private int numVertices, numIndices, precision;
    private int[] indices;
    private Vector3f[] vertices;
    private Vector2f[] texCoords;
    private Vector3f[] normals;

    public Sphere(int p) {
        precision = p;
        initSphere();
    }

    private void initSphere() {
        numVertices = (precision + 1) * (precision + 1);
        numIndices = precision * precision * 6;
        indices = new int[numIndices];
        vertices = new Vector3f[numVertices];
        texCoords = new Vector2f[numVertices];
        normals  = new Vector3f[numVertices];
        for (int i = 0; i < numVertices; i++) {
            vertices[i] = new Vector3f();
            texCoords[i] = new Vector2f();
            normals[i] = new Vector3f();
        }
        for (int i = 0; i <= precision; i++) {
            for (int j = 0; j <= precision; j++) {
                float y = (float) cos(toRadians(180 - i * 180 / precision));
                float x = -(float) cos(toRadians(j * 360 / (float) precision)) * (float) abs(cos(asin(y)));
                float z = (float) sin(toRadians(j * 360 / (float) precision)) * (float) abs(cos(asin(y)));
                vertices[i * (precision + 1) + j].set(x, y, z);
                texCoords[i * (precision + 1) + j].set((float) j / precision, (float) i / precision);
                normals[i * (precision + 1) + j].set(x, y, z);
            }
        }
        for (int i = 0; i < precision; i++) {
            for (int j = 0; j < precision; j++){
                indices[6 * (i * precision + j) + 0] = i * (precision + 1) + j;
                indices[6 * (i * precision + j) + 1] = i * (precision + 1) + j + 1;
                indices[6 * (i * precision + j) + 2] = (i + 1) * (precision + 1) + j;
                indices[6 * (i * precision + j) + 3] = i * (precision + 1) + j + 1;
                indices[6 * (i * precision + j) + 4] = (i + 1) * (precision + 1) + j + 1;
                indices[6 * (i * precision + j) + 5] = (i + 1) * (precision + 1) + j;
            }
        }
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
