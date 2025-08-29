package fc.PrintingApplication.TP2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

import javax.imageio.ImageIO;

import fc.Math.AABB;
import java.awt.Color;

public class Remplissage 
{
        ArrayList<Arete> listArete;
        AABB aabb;
        public Remplissage(ArrayList<Arete> l, AABB box)
        {
            listArete = l;
            aabb = box;
        }

        public void scanLine(String filename, int compteur)
	{
		BufferedImage image = new BufferedImage(1500, 1500, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
		g2d.setColor(Color.WHITE);

		listArete.sort(Comparator.comparing(a -> a.First.y));	
		for(int y = (int)aabb.getMin().y * 20; y < (int)aabb.getMax().y * 20; y++)
		{
			ArrayList<Arete> intersectlistArete = new ArrayList<>();
			ArrayList<Intersection> intersections = new ArrayList<>();
			for(int i = 0; i < listArete.size(); i++)
			{
				if(listArete.get(i).First.y * 20 > y) break;
				
				if(listArete.get(i).Second.y != listArete.get(i).First.y && listArete.get(i).Second.y * 20 > y) intersectlistArete.add(listArete.get(i));
			}
			

			for(int i = 0; i < intersectlistArete.size(); i++)
			{
				float pente = (intersectlistArete.get(i).Second.x - intersectlistArete.get(i).First.x) / (intersectlistArete.get(i).Second.y - intersectlistArete.get(i).First.y);
				intersections.add(new Intersection(y, (int) (intersectlistArete.get(i).First.x * 20 + pente * (y - intersectlistArete.get(i).First.y * 20)),intersectlistArete.get(i)));
			}
			intersections.sort(Comparator.comparing(a -> a.xIntersect));

		for (int i = 0; i < intersections.size(); i +=2)
		{
			g2d.drawLine((int)(intersections.get(i).xIntersect) + image.getWidth() / 2, y + image.getHeight() / 2, (int)(intersections.get(i + 1).xIntersect) + image.getWidth() / 2, y + image.getHeight() / 2);
		} 
		}


		File directory = new File("TP2/Results/" + filename + "/remplissage2");
	
		if (!directory.exists()) directory.mkdir();

		try { ImageIO.write(image, "png", new File( "TP2/Results/" +filename +"/remplissage" + "2/" + String.valueOf(compteur) + ".png"));}
		catch (Exception e) { e.printStackTrace() ;}
	}
}
