package fc.PrintingApplication.TP2;



import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.parser.Parse;

import fc.ChewTriangulation.Pnt;
import fc.ChewTriangulation.Triangulation;
import fc.GLObjects.GLProgram;
import fc.GLObjects.GLRenderTarget;
import fc.GLObjects.GLShaderMatrixParameter;
import fc.Math.AABB;
import fc.Math.Matrix;
import fc.Math.Plane;
import fc.Math.Vec2f;
import fc.Math.Vec2i;
import fc.Math.Vec3f;
import fc.Math.Vec4f;
import javafx.scene.shape.Mesh;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.*;
import java.util.List;

import java.nio.file.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Stream;


    public class Main
{


	//
	// Dans ce TP, lire et parser un fichier OBJ
	// puis realiser des coupes par plan Z.
	// Une fois les perimetres extraits:
	// - effectuer une triangulation de Delaunay contrainte.
	// - rasteriser dans une bitmap. Se r�f�rer � l'�nonc� du TP pour tous les d�tails.
	//


	public static int width = 2000;		// image width
	public static int height = 2000;	// image height
	public static float buseDiameter = 0.4f;	// diamètre de la buse
	public static float resolution = 0.05f;	// resolution
	public static float k = 3f;		// augmenter / dimuner la distance entre chaque path
	public static Object object = Object.YODA;	// object to choose
	public static Render renderType = Render.STENCIL;	// afficher les chemins ou le depth peeling

	public static float step = 0.2f;	// pas entre tranche


	public enum Render
	{
		CHEMIN("Chemin"), STENCIL("Stencil"), BLENDING("Blending");

		private final String path;

		Render(String path) 
		{
			this.path = path;
		}
		public String getPath() 
		{
		return path;
		}

	}

	public enum Object
	{
		CUBE("3d_models/cube.obj"), 
		CUBEWITHHOLE("3d_models/Cubewithhole.obj"),
		CUTEOCTO("3d_models/CuteOcto.obj"),
		GIRAFFE("3d_models/giraffe.obj"), 
		MOAI("3d_models/moai.obj"), 
		YODA("3d_models/yoda.obj");

		private final String path;

		Object(String path) 
		{
			this.path = path;
		}
		public String getPath() 
		{
		return path;
		}
	}



public static void main(String[] args) 
	{
		Main main = new Main();
		
		File f = new File("Results");
		if (!f.exists()) f.mkdir();
		
		String file = object.path.substring(object.path.lastIndexOf("/") + 1);
		file = file.substring(0, file.lastIndexOf("."));


		File baseDir = new File("Results/" + file);
		if (!baseDir.exists()) baseDir.mkdirs(); 

		File contourDir = new File(baseDir, renderType.path);
		if (contourDir.exists()) {
			
			File[] files = contourDir.listFiles();
			if (files != null) {
				for (File f2 : files) f2.delete();
			}
		} else {
			contourDir.mkdir();
		}
		main.parseObjFile();

	}


	public void parseObjFile()
	{
	    try
	    {
	        Build builder = new Build();
	        Parse obj = new Parse(builder, new File(object.path).toURI().toURL());
	        AABB aabb = new AABB();
	        
	        // Enumeration des sommets
			
	        
	        for (FaceVertex vertex : builder.faceVerticeList)
	        {
	        	float x = vertex.v.x;
		    	float y = vertex.v.y;
	        	float z = vertex.v.z;
	        	
				aabb.enlarge(new Vec3f(x, y, z));
	        }
			aabb.addMargin(new Vec3f(10, 10, 0));
	        // Enumeration des faces (souvent des triangles, mais peuvent comporter plus de sommets dans certains cas)
	        int compteur = 0;
			//setupGL();
			//Trancher(builder, aabb, (float)(aabb.getMin().z + 0.2), filename, compteur);


			switch (renderType) 
			{
				case CHEMIN:
					for(float Z = aabb.getMin().z; Z <= aabb.getMax().z; Z += Main.step)
					{
						Trancher(builder, aabb, Z, compteur);
						compteur++;
					}
					break;
				case BLENDING:
					
					break;
				case STENCIL:
					setupGL();
					break;
				default:
					break;
			}


	    }
	    catch (java.io.FileNotFoundException e)
	    {
	    	System.out.println("FileNotFoundException loading file "+object.path+", e=" + e);
	        e.printStackTrace();
	    }
	    catch (java.io.IOException e)
	    {
	    	System.out.println("IOException loading file "+object.path+", e=" + e);
	        e.printStackTrace();
	    }
	}

	

	void Trancher(Build build, AABB aabb, float z, int compteur)
	{
		Plane plane = new Plane(new Vec3f(aabb.getCenter().x, aabb.getCenter().y, z), new Vec3f(0, 0, 1));
		ArrayList<Arete> listIntersect = new ArrayList<>();
		for (Face face : build.faces)
		{
			// Parcours des triangles de cette face
			
			for (int i=1; i <= (face.vertices.size() - 2); i++)
			{
				int vertexIndex1 = face.vertices.get(0).index;
				int vertexIndex2 = face.vertices.get(i).index;
				int vertexIndex3 = face.vertices.get(i+1).index;
				
				FaceVertex vertex1 = build.faceVerticeList.get(vertexIndex1);
				FaceVertex vertex2 = build.faceVerticeList.get(vertexIndex2);
				FaceVertex vertex3 = build.faceVerticeList.get(vertexIndex3);
				
				// ...

				Vec2f p1;
				Vec2f p2;
				if(plane.isAbove(vertex1.v.z) != plane.isAbove(vertex2.v.z) || plane.isAbove(vertex1.v.z) != plane.isAbove(vertex3.v.z) || plane.isAbove(vertex2.v.z) != plane.isAbove(vertex3.v.z))
					// intersection
				{
					if(!plane.crossPlane(vertex1.v.z, vertex2.v.z))
					{
						p1 = plane.intersectionPoint(new Vec3f(vertex2.v.x, vertex2.v.y, vertex2.v.z), new Vec3f(vertex3.v.x, vertex3.v.y, vertex3.v.z));
						p2 = plane.intersectionPoint(new Vec3f(vertex1.v.x, vertex1.v.y, vertex1.v.z), new Vec3f(vertex3.v.x, vertex3.v.y, vertex3.v.z));
					}

					else if(!plane.crossPlane(vertex1.v.z, vertex3.v.z))
					{
						p1 = plane.intersectionPoint(new Vec3f(vertex1.v.x, vertex1.v.y, vertex1.v.z), new Vec3f(vertex2.v.x, vertex2.v.y, vertex2.v.z));
						p2 = plane.intersectionPoint(new Vec3f(vertex2.v.x, vertex2.v.y, vertex2.v.z), new Vec3f(vertex3.v.x, vertex3.v.y, vertex3.v.z));
					}

					else
					{
						p1 = plane.intersectionPoint(new Vec3f(vertex1.v.x, vertex1.v.y, vertex1.v.z), new Vec3f(vertex2.v.x, vertex2.v.y, vertex2.v.z));
						p2 = plane.intersectionPoint(new Vec3f(vertex1.v.x, vertex1.v.y, vertex1.v.z), new Vec3f(vertex3.v.x, vertex3.v.y, vertex3.v.z));
					}
					listIntersect.add(new Arete(p1, p2));
				}

				

			}
		}
		String file = object.path.substring(object.path.lastIndexOf("/") + 1);
		file = file.substring(0, file.lastIndexOf("."));
		if(!listIntersect.isEmpty())
		{
			
			Tranche t = new Tranche(listIntersect);
			
			
			if(t.listContours.size() > 0)
			{
				//System.out.println(t.listContours.size());
				//BufferedImage img = t.dessinerContoursImage(Main.width, Main.height, 20, Main.width/2, Main.height/2, 0);
				BufferedImage img = t.dessinerContoursEtOffsetsImage(Main.width, Main.height, Main.resolution, Main.width/2, Main.height/2);
				try { ImageIO.write(img, "png", new File( "Results/" + file +"/" + renderType.path + "/" + String.valueOf(compteur) + ".png")); }
				catch (Exception e) { e.printStackTrace() ;}
			}

		}
	}	



	void setupGL()
	{
		GLFW.glfwInit();

		GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		GLFW.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		GLFW.glfwWindowHint(GLFW_SAMPLES, 0);

		long win = GLFW.glfwCreateWindow(width, height, "Peeling", 0, 0);
		GLFW.glfwMakeContextCurrent(win);
		GL.createCapabilities();
		glViewport(0, 0, width, height);

		Build builder = new Build();
		try
	    {
			Parse obj = new Parse(builder, new File(object.path).toURI().toURL());
			AABB aabb = new AABB();
			for (FaceVertex vertex : builder.faceVerticeList)
			{
				float x = vertex.v.x;
				float y = vertex.v.y;
				float z = vertex.v.z;
				
				aabb.enlarge(new Vec3f(x, y, z));
			}
			aabb.addMargin(new Vec3f(10, 10, 0));

			//	1) Construire le VAO
			float Wmm = width  * resolution;          // largeur monde couverte par l'image
			float Hmm = height * resolution;          // hauteur monde couverte
			float x0  = aabb.getCenter().x - Wmm*0.5f; // bord gauche du domaine
			float y0  = aabb.getCenter().y - Hmm*0.5f; // bord bas du domaine

			java.util.function.DoubleUnaryOperator toNdcX = x -> ((x - x0)/Wmm)*2.0 - 1.0;
			java.util.function.DoubleUnaryOperator toNdcY = y -> ((y - y0)/Hmm)*2.0 - 1.0;
			ArrayList<Float> pos = new java.util.ArrayList<>();
			ArrayList<Integer> idx = new java.util.ArrayList<>();
			int running = 0;


			for (Face f : builder.faces) 
			{
				int n = f.vertices.size();
				if (n < 3) continue;
				int i0 = f.vertices.get(0).index;
				float x0w = builder.faceVerticeList.get(i0).v.x;
				float y0w = builder.faceVerticeList.get(i0).v.y;
				float z0w = builder.faceVerticeList.get(i0).v.z;
				for (int i = 1; i < n-1; i++) {
					int i1 = f.vertices.get(i).index;
					int i2 = f.vertices.get(i+1).index;
					float x1w = builder.faceVerticeList.get(i1).v.x, y1w = builder.faceVerticeList.get(i1).v.y, z1w = builder.faceVerticeList.get(i1).v.z;
					float x2w = builder.faceVerticeList.get(i2).v.x, y2w = builder.faceVerticeList.get(i2).v.y, z2w = builder.faceVerticeList.get(i2).v.z;

					// v0
					pos.add((float)toNdcX.applyAsDouble(x0w)); pos.add((float)toNdcY.applyAsDouble(y0w)); pos.add(z0w);
					// v1
					pos.add((float)toNdcX.applyAsDouble(x1w)); pos.add((float)toNdcY.applyAsDouble(y1w)); pos.add(z1w);
					// v2
					pos.add((float)toNdcX.applyAsDouble(x2w)); pos.add((float)toNdcY.applyAsDouble(y2w)); pos.add(z2w);

					idx.add(running+0); idx.add(running+1); idx.add(running+2);
					running += 3;
				}
			}

			float minX=1e9f,maxX=-1e9f,minY=1e9f,maxY=-1e9f;
			for (int i=0;i<pos.size(); i+=3) {
				float X=pos.get(i), Y=pos.get(i+1);
				if(X<minX)minX=X; if(X>maxX)maxX=X;
				if(Y<minY)minY=Y; if(Y>maxY)maxY=Y;
			}
			System.out.println("NDC X=["+minX+","+maxX+"], Y=["+minY+","+maxY+"]");

			int vao = glGenVertexArrays(); glBindVertexArray(vao);
			int vbo = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER, vbo);


			float[] posArr = new float[pos.size()]; for (int i=0;i<pos.size();i++) posArr[i]=pos.get(i);
			glBufferData(GL_ARRAY_BUFFER, posArr, GL_STATIC_DRAW);
			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 3, GL_FLOAT, false, 3*Float.BYTES, 0L);


			int ebo = glGenBuffers(); glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
			// convertir ArrayList -> int[]
			int[] idxArr = new int[idx.size()]; for (int i=0;i<idx.size();i++) idxArr[i]=idx.get(i);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxArr, GL_STATIC_DRAW);
			glBindVertexArray(0);

			//2 Instancier et initialiser le peeler

			DepthPeelingGPU dp = new DepthPeelingGPU(width, height, /*sXY=*/resolution,
											aabb.getMin().z, aabb.getMax().z);
			dp.setMesh(vao, idxArr.length, GL_TRIANGLES);
			dp.initGL();

			float zMin = aabb.getMin().z;
			float zMax = aabb.getMax().z;
			float zSlice = 0.5f * (zMin + zMax);   // par ex. coupe au milieu
			dp.renderSlice(zSlice);                 // <-- c’est ici
			glFinish();   

			// Option : éviter le sRGB pour une sortie brute 8-bit
			glDisable(GL_FRAMEBUFFER_SRGB);



			final float step = 0.20f;    // mm par couche (à adapter)
			final float epsZ = 1e-6f;    // petite marge

			// construire le dossier de sortie cohérent avec <file>/<renderType.path>
			String file = object.path.substring(object.path.lastIndexOf("/") + 1);
			file = file.substring(0, file.lastIndexOf("."));
			java.nio.file.Path outDir = java.nio.file.Paths.get("Results", file, renderType.path);
			java.nio.file.Files.createDirectories(outDir);

			// nombre de couches
			int nSlices = (int)Math.floor((zMax - zMin) / step) + 1;
			// padding pour noms 0000.png, 0001.png, ...
			int pad = Integer.toString(Math.max(0, nSlices - 1)).length();

			for (int k = 0; k < nSlices; ++k) {
				zSlice = zMin + k * step;
				if (zSlice > zMax + epsZ) break;

				// Option : échantillonner au centre de la tranche
				// zSlice = Math.min(zMax, zSlice + 0.5f * step);

				dp.renderSlice(zSlice);

				glBindFramebuffer(GL_READ_FRAMEBUFFER, dp.getFbo()); // voir remarque ci-dessous
				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
				glReadBuffer(GL_COLOR_ATTACHMENT0);
				glBlitFramebuffer(
					0, 0, width, height,
					0, 0, width, height,
					GL_COLOR_BUFFER_BIT, GL_NEAREST
				);
				GLFW.glfwSwapBuffers(win);
				GLFW.glfwPollEvents();

				glFinish(); // s'assurer que le GPU a terminé avant lecture

				BufferedImage img = readColorTextureRGBA8(dp.getColorTexture(), width, height);
				String name = String.format("tranche%0" + pad + "d.png", k);
				ImageIO.write(img, "png", outDir.resolve(name).toFile());
			}
		}

		catch (java.io.FileNotFoundException e)
	    {
	    	System.out.println("FileNotFoundException loading file "+object.path+", e=" + e);
	        e.printStackTrace();
	    }
	    catch (java.io.IOException e)
	    {
	    	System.out.println("IOException loading file "+object.path+", e=" + e);
	        e.printStackTrace();
	    }	
	}
	



	
private static BufferedImage readColorTextureRGBA8(int tex, int w, int h) {
    // Lire la texture directement
    glBindTexture(GL_TEXTURE_2D, tex);
    java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
    glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);

    // Construire BufferedImage (flip vertical)
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    int stride = w * 4;
    for (int y = 0; y < h; ++y) {
        int srcY = (h - 1 - y); // flip
        int row = srcY * stride;
        for (int x = 0; x < w; ++x) {
            int i = row + x * 4;
            int r = buf.get(i)   & 0xFF;
            int g = buf.get(i+1) & 0xFF;
            int b = buf.get(i+2) & 0xFF;
            int a = buf.get(i+3) & 0xFF;
            int argb = (a<<24) | (r<<16) | (g<<8) | b;
            img.setRGB(x, y, argb);
        }
    }
    return img;
}
}
