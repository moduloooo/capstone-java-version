import static com.jogamp.opengl.GL4.*;

import com.jogamp.common.util.IOUtil;
import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.jogamp.opengl.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.Graphics2D;
import java.nio.IntBuffer;
import java.util.Scanner;
import java.util.Vector;
import java.awt.geom.AffineTransform;

import java.awt.image.WritableRaster;

import javax.imageio.ImageIO;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

//import jogamp.graph.geom.plane.AffineTransform;
import jogamp.opengl.GLContextImpl;
import java.awt.image.BufferedImage;


public class Utils {

    public Utils () {

    }
    private static String[] readShaderSource(String filename) {
        Vector<String> lines = new Vector<String>();
        Scanner sc; 
        try {
            sc = new Scanner(new File(filename));
        } catch (IOException e) {
            System.err.println("IOException reading file: " + e);
            return null;
        }
        while (sc.hasNext()) {
            lines.addElement(sc.nextLine());
        }
        String[] program = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            program[i] = (String) lines.elementAt(i) + "\n";
        }
        return program;
    }

    public static int createShaderProgram(String verShader, String fraShader) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        int[] vertCompiled = new int[1];
        int[] fragCompiled = new int[1];
        int[] linked = new int[1];

        String vShaderSource[] = readShaderSource(verShader);
        String fShaderSource[] = readShaderSource(fraShader);
        
        int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
        gl.glShaderSource(vShader, vShaderSource.length, vShaderSource, null, 0);
        gl.glCompileShader(vShader);
        checkOpenGLError();
        gl.glGetShaderiv(vShader, GL_COMPILE_STATUS, vertCompiled, 0);
        if (vertCompiled[0] == 1) {
            System.out.println(". . . vertex compilation success.");
        } else {
            System.out.println(". . . vertex compilation failed");
            printShaderLog(vShader);
        }

        int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
        gl.glShaderSource(fShader, fShaderSource.length, fShaderSource, null, 0);
        gl.glCompileShader(fShader);
        checkOpenGLError();
        gl.glGetShaderiv(fShader, GL_COMPILE_STATUS, fragCompiled, 0);
        if (fragCompiled[0] == 1) {
            System.out.println(". . . fragment compilation success.");
        } else {
            System.out.println(". . . fragment compilation failed");
            printShaderLog(fShader);
        }

        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        gl.glLinkProgram(vfprogram);
        checkOpenGLError();
        gl.glGetProgramiv(vfprogram, GL_LINK_STATUS, linked, 0);
        if (linked[0] == 1) {
            System.out.println(". . . linking succeeded");
        } else {
            System.out.println(". . . linking failed");
            printProgramLog(vfprogram);
        }

        gl.glDeleteShader(vShader);
        gl.glDeleteShader(fShader);
        return vfprogram;
    }

    private static void printShaderLog(int shader) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] len = new int[1];
        int[] chWrittn = new int[1];
        byte[] log = null;
        
        gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0);
        if (len[0] > 0) {
            log = new byte[len[0]];
            gl.glGetShaderInfoLog(shader, len[0], chWrittn, 0, log, 0);
            System.out.println("Shader Info Log: ");
            for (int i = 0; i < log.length; i++) {
                System.out.print((char) log[i]);
            }
        }
    }

    static void printProgramLog(int prog) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] len = new int[1];
        int[] chWrittn = new int[1];
        byte[] log = null;

        gl.glGetProgramiv(prog, GL_INFO_LOG_LENGTH, len, 0);
        if (len[0] > 0) {
            log = new byte[len[0]];
            gl.glGetProgramInfoLog(prog, len[0], chWrittn, 0, log, 0);
            for (int i = 0; i < log.length; i++) {
                System.out.print((char) log[i]);
            }
        }
    } 

    static boolean checkOpenGLError() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        boolean foundError = false;
        GLU glu = new GLU();
        int glErr = gl.glGetError();
        while (glErr != GL_NO_ERROR) {
            System.err.println("glError: " + glu.gluErrorString(glErr));
            foundError = true;
            glErr = gl.glGetError();
        }
        return foundError;
    }

    public static float[] silverAmbient() {
        return (new float[] {0.25f, 0.20725f, 0.20725f, 0.922f});
    }

    public static float[] silverDiffuse() {
        return (new float[] {1.00f, 0.829f, 0.829f, 0.922f});
    }

    public static float[] silverSpecular() {
        return (new float[] {0.2f, 0.2966f, 0.2966f, 0.922f});
    }

    public static float silverShininess() {
        return 51.2f;
    }

    public static int loadTexture(String textureFileName) {
        Texture tex = null;
        try {
            tex = TextureIO.newTexture(new File(textureFileName), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int textureID = tex.getTextureObject();
        return textureID;
    }

    public static Texture loadTexture2(String file) {
        Texture tex = null;
        try {
            tex = TextureIO.newTexture(new File (file), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tex;
    }

    private static BufferedImage getBufferedImage(String fileName) { 
        BufferedImage img;
        try { 
            img = ImageIO.read(new File(fileName)); 
        } catch (IOException e) { 
            System.err.println("Error reading '" + fileName + '"'); throw new RuntimeException(e); 
        }
        return img;
    }

    private static byte[ ] getRGBAPixelData(BufferedImage img, boolean flip) { 
        byte[ ] imgRGBA;
        int height = img.getHeight(null);
        int width = img.getWidth(null);
        WritableRaster raster = WritableRaster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);
        ComponentColorModel colorModel = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            new int[ ] { 8, 8, 8, 8 }, true, false, 
            ComponentColorModel.TRANSLUCENT, // transparency
            DataBuffer.TYPE_BYTE); // data transfer type
        BufferedImage newImage = new BufferedImage(colorModel, raster, false, null);
        Graphics2D g = newImage.createGraphics();
        if (flip) { 
            AffineTransform gt = new AffineTransform();
            gt.translate(0, height);
            gt.setToScale(1, (float) -1d);
            g.setTransform(gt);
        }
        g.drawImage(img, null, null);
        g.dispose();
        DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
        imgRGBA = dataBuf.getData();
        return imgRGBA;
    }

    public static int loadCubeMap(String file) {
        GL4 gl = (GL4)GLContext.getCurrentGL();
        String topFile = file + File.separator + "1top.jpg";
        String leftFile = file + File.separator + "6left.jpg";
        String backFile = file + File.separator + "4back.jpg";
        String rightFile = file + File.separator + "5right.jpg";
        String frontFile = file + File.separator + "3front.jpg";
        String bottomFile = file + File.separator + "2bot.jpg";
        BufferedImage topImage = getBufferedImage(topFile);
        BufferedImage leftImage = getBufferedImage(leftFile);
        BufferedImage frontImage = getBufferedImage(frontFile);
        BufferedImage rightImage = getBufferedImage(rightFile);
        BufferedImage backImage = getBufferedImage(backFile);
        BufferedImage bottomImage = getBufferedImage(bottomFile);

        byte[ ] topRGBA = getRGBAPixelData(topImage, false);
        byte[ ] leftRGBA = getRGBAPixelData(leftImage, false);
        byte[ ] frontRGBA = getRGBAPixelData(frontImage, false);
        byte[ ] rightRGBA = getRGBAPixelData(rightImage, false);
        byte[ ] backRGBA = getRGBAPixelData(backImage, false);
        byte[ ] bottomRGBA = getRGBAPixelData(bottomImage, false);

        ByteBuffer topWrappedRGBA = ByteBuffer.wrap(topRGBA);
        ByteBuffer leftWrappedRGBA = ByteBuffer.wrap(leftRGBA);
        ByteBuffer frontWrappedRGBA = ByteBuffer.wrap(frontRGBA);
        ByteBuffer rightWrappedRGBA = ByteBuffer.wrap(rightRGBA);
        ByteBuffer backWrappedRGBA = ByteBuffer.wrap(backRGBA);
        ByteBuffer bottomWrappedRGBA = ByteBuffer.wrap(bottomRGBA);

        int[ ] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);
        gl.glTexStorage2D(GL_TEXTURE_CUBE_MAP, 1, GL_RGBA8, 640, 640);

        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, rightWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, leftWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, bottomWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, topWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, frontWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, 640, 640,
            GL_RGBA, GL.GL_UNSIGNED_BYTE, backWrappedRGBA);

        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        return textureID;
    }

    public static Texture loadImage(String file) {
        Texture tex = null;
        try {
            tex = TextureIO.newTexture(new File(file), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tex;
    }
}
