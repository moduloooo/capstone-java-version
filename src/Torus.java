import static java.lang.Math.*;
import org.joml.*;

public class Torus {
    private int numVertices, numIndices, prec;
    private int[ ] indices;
    private Vector3f[ ] vertices;
    private Vector2f[ ] texCoords;
    private Vector3f[ ] normals;
    private float inner, outer;
    private Vector3f[ ] sTangents, tTangents;

    public Torus() {
        prec = 48;
        inner = 0.5f;
        outer = 0.2f;
        initTorus();
    }

	 public Torus(float innerRadius, float outerRadius, int precision) {	 
        inner = innerRadius; outer = outerRadius; prec = precision;
        initTorus();
    }

    private void initTorus() { 
        numVertices = (prec+1) * (prec+1);
        numIndices = prec * prec * 6;
        indices = new int[numIndices];
        vertices = new Vector3f[numVertices];
        texCoords = new Vector2f[numVertices];
        normals = new Vector3f[numVertices];
        sTangents = new Vector3f[numVertices];
        tTangents = new Vector3f[numVertices];
        for (int i=0; i<numVertices; i++) { 
            vertices[i] = new Vector3f();
            texCoords[i] = new Vector2f();
            normals[i] = new Vector3f();
            sTangents[i] = new Vector3f();
            tTangents[i] = new Vector3f();
        }
	 	 // calculate first ring.
        for (int i=0; i<prec+1; i++) {	 
            float amt = (float) toRadians(i*360.0f/prec);
            // build the ring by rotating points around the origin, then moving them outward
            Vector3f ringPos = new Vector3f(0.0f, outer, 0.0f);
            ringPos.rotateAxis(amt, 0.0f, 0.0f, 1.0f);
            ringPos.add(new Vector3f(inner, 0.0f, 0.0f));
            vertices[i].set(ringPos);
            // compute texture coordinates for each vertex in the ring
            texCoords[i].set(0.0f, ((float)i)/((float)prec));
            // compute tangents and normal vectors for each vertex in the ring
            tTangents[i] = new Vector3f(0.0f, -1.0f, 0.0f);	 // The first tangent vector starts as the -Y axis,
            tTangents[i].rotateAxis(amt+(3.14159f/2.0f), 0.0f, 0.0f, 1.0f);
            // and is then rotated around the Z axis.
            sTangents[i].set(0.0f, 0.0f, -1.0f);	 // The second tangent is -Z in each case.
            normals[i] = tTangents[i].cross(sTangents[i]); // The cross product produces the normal
        }
        // rotate the first ring about the Y axis to get the other rings
        for (int ring=1; ring<prec+1; ring++) { 
            for (int vert=0; vert<prec+1; vert++) { 
                // rotate the vertex positions of the original ring around the Y axis
                float amt = (float) toRadians((float)ring*360.0f/prec);
                Vector3f vp = new Vector3f(vertices[vert]);
                vp.rotateAxis(amt, 0.0f, 1.0f, 0.0f);
                vertices[ring*(prec+1)+vert].set(vp);
                // compute the texture coordinates for the vertices in the new rings
                texCoords[ring*(prec+1)+vert].set((float)ring*2.0f/(float)prec, texCoords[vert].y());
                // rotate the tangent and bitangent vectors around the Y axis
                sTangents[ring*(prec+1)+vert].set(sTangents[vert]);
                sTangents[ring*(prec+1)+vert].rotateAxis(amt, 0.0f, 1.0f, 0.0f);
                tTangents[ring*(prec+1)+vert].set(tTangents[vert]);
                tTangents[ring*(prec+1)+vert].rotateAxis(amt, 0.0f, 1.0f, 0.0f);
                                // rotate the normal vector around the Y axis
                normals[ring*(prec+1)+vert].set(normals[vert]);
                normals[ring*(prec+1)+vert].rotateAxis(amt, 0.0f, 1.0f, 0.0f);
            } 
        }
        // calculate triangle indices corresponding to the two triangles built per vertex
        for(int ring=0; ring<prec; ring++) { 
            for(int vert=0; vert<prec; vert++) { 
                indices[((ring*prec+vert)*2) *3+0]= ring*(prec+1)+vert;
                indices[((ring*prec+vert)*2) *3+1]=(ring+1)*(prec+1)+vert;
                indices[((ring*prec+vert)*2) *3+2]= ring*(prec+1)+vert+1;
                indices[((ring*prec+vert)*2+1)*3+0]= ring*(prec+1)+vert+1;
                indices[((ring*prec+vert)*2+1)*3+1]=(ring+1)*(prec+1)+vert;
                indices[((ring*prec+vert)*2+1)*3+2]=(ring+1)*(prec+1)+vert+1; 
            } 
        }
    }   
    // accessors for the torus indices and vertices
    int getNumIndices() { return numIndices; }

    public int[ ] getIndices() { return indices; }

    public int getNumVertices() { return numVertices; }

    public Vector3f[ ] getVertices() { return vertices; }

    public Vector2f[ ] getTexCoords() { return texCoords; }

    public Vector3f[ ] getNormals() { return normals; }

    public Vector3f[ ] getStangents() { return sTangents; }

    public Vector3f[ ] getTtangents() { return tTangents; }

}
