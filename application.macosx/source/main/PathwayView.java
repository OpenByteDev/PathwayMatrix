package main;

import java.awt.Color;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import main.PathwayViewer_2_6.ThreadLoader4;

import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.SmallMolecule;

import edu.uic.ncdm.venn.Venn_Overview;

import processing.core.PApplet;
import GraphLayout.*;


public class PathwayView{
	public PApplet parent;
	public ArrayList<String> files;
	public static int nFiles;
	public boolean isAllowedDrawing = false;
	
	// Read data 
	public Map<String,String> mapProteinRDFId;
	public Map<String,String> mapSmallMoleculeRDFId;
	public Map<String,String> mapComplexRDFId;
	public Map<String,Complex> mapComplexRDFId_Complex;
	public Set<SmallMolecule> smallMoleculeSet;
	
	public ArrayList<String> complexList = new ArrayList<String>(); 
	public ArrayList<String> proteinList = new ArrayList<String>();
	
	public ArrayList<Integer> rectSizeList;
	public ArrayList<Integer> rectFileList;
	
	
	public int maxSize = 0;
	public static Gradient gradient = new Gradient();
	public static float colorScale=0;
	public static Integrator[][] iS;
	public static float xCircular, yCircular, rCircular; 
	
	
	public static Graph g;
	public static float xRight =0;
	public Slider2 slider2;
	public static PopupLayout popupLayout;
	public static CheckBox checkName;
	public ThreadLoader5 loader5;
	public Thread thread5 = new Thread(loader5);
	
	
	// position
	public static float[] yTopological;
	public static float[] yTree;
	public static Integrator iTransition = new Integrator(0,0.1f,0.4f);
	
	// Hierarchy 
	public static Pathway2[] filePathway = null;
	public static Pathway2 rootPathway = null;
	public PopupPathway popupPathway;
	public static Pathway2 bPathway;
	
	public static float scale=1;
	boolean isBrushing =false;
	public static int setIntegrator =0;
	
	// Button to control the map
	public ButtonMap buttonReset;
	public ButtonMap buttonExpand;
	public ButtonMap buttonCollapse;
	
	public PathwayView(PApplet p){
		parent = p;
		buttonReset = new ButtonMap(parent);
		buttonExpand = new ButtonMap(parent);
		buttonCollapse = new ButtonMap(parent);
		
		loader5= new ThreadLoader5(parent);
		slider2 = new Slider2(parent);
		popupLayout = new PopupLayout(parent);
		checkName = new CheckBox(parent,"Reactions names");
		popupPathway = new PopupPathway(parent);
		
		xRight = parent.width*7.5f/10;
		float v=0.5f;
		gradient.addColor(new Color(0,0,v));
		gradient.addColor(new Color(0,v,v));
		gradient.addColor(new Color(0,v,0));
		gradient.addColor(new Color(v,v,0));
		gradient.addColor(new Color(v,0,0));
		gradient.addColor(new Color(v,0,v));
		gradient.addColor(new Color(0,0,v));
		
	}
	
	public void setItems(ArrayList<BiochemicalReaction> rectList){
		// Causality integrator
		iS = new Integrator[rectList.size()][rectList.size()];
		for (int i=0;i<rectList.size();i++){
			for (int j=0;j<rectList.size();j++){
				iS[i][j] = new Integrator(0, 0.2f,SliderSpeed.speed/2);
			}
		}	
		
		// Compute proteinList and complexList
		complexList = new ArrayList<String>(); 
		proteinList = new ArrayList<String>();
		for (int r=0; r<rectList.size();r++){
			BiochemicalReaction react = rectList.get(r);
			Object[] left = react.getLeft().toArray();
			Object[] right = react.getRight().toArray();
			for (int i=0;i<left.length;i++){
				String ref = left[i].toString();
				  if ( mapProteinRDFId.get(ref)!=null){
					  String proteinName = mapProteinRDFId.get(ref);
					  if (!proteinList.contains(proteinName))
							proteinList.add(proteinName);
				  }	  
				  else if (mapComplexRDFId.get(ref)!=null){
					  String complexName = mapComplexRDFId.get(ref);
					  if (!complexList.contains(complexName))
						  complexList.add(complexName);
				  }
			}
			for (int i=0;i<right.length;i++){
				String ref = right[i].toString();
				  if ( mapProteinRDFId.get(ref)!=null){
					  String proteinName = mapProteinRDFId.get(ref);
					  if (!proteinList.contains(proteinName))
							proteinList.add(proteinName);
				  }	  
				  else if (mapComplexRDFId.get(ref)!=null){
					  String complexName = mapComplexRDFId.get(ref);
					  if (!complexList.contains(complexName))
						  complexList.add(complexName);
				  }
			}
		}
		
		// Compute size of reaction
		maxSize =0;
		for (int r=0; r<rectList.size();r++){
			BiochemicalReaction react = rectList.get(r);
			Object[] left = react.getLeft().toArray();
			Object[] right = react.getRight().toArray();
			
			ArrayList<Integer> proteinsL = getProteinsInOneSideOfReaction(left);
			ArrayList<Integer> proteinsR = getProteinsInOneSideOfReaction(right);
			int size = proteinsL.size()+ proteinsR.size();
			rectSizeList.add(size);   
			if (size>maxSize)
				maxSize = size;
		}
			
		colorScale = (float) (gradient.colors.size()-0.8f)/ (nFiles) ;
		isAllowedDrawing = true;
		resetPosistion();
		updateScale();
	}
	
	
	public void updateScale() {
		float countReactions=0;
		for (int i=0;i<filePathway.length;i++){
			countReactions+=filePathway[i].numReactions;
		}
		rCircular = PApplet.pow(countReactions,0.55f)*10f*scale;
	}
		
	public void resetPosistion() {
		xCircular = xRight/2;
		yCircular = parent.height/2;
	}
	
	public void updateNodes(ArrayList<BiochemicalReaction> rectList) {
		g = new Graph();
		for (int i = 0; i < rectList.size(); i++) {
			int fileId = rectFileList.get(i);
			Node node = new Node(new Vector3D( 20+parent.random(xRight-40), 20 + parent.random(parent.height-40), 0), parent) ;
			node.setMass(6+PApplet.pow(rectSizeList.get(i),0.7f));
			node.nodeId = i;
			node.reaction = rectList.get(i);
			node.color = getColor(fileId);//gradient.getGradient(colorScale*(transferID(fileId)));
			g.addNode(node);
		}	
		// Initialize topological ordering
		orderTree();
		yTopological =  new float[rectList.size()];
		orderTopological();
	}
	public static Color getColor(int fileId) {
		return gradient.getGradient(colorScale*(transferID(fileId)));
	}
		
	
	// Make sure pathways next to each other receive different colora
	public static float transferID(int id) {
		float newId = (id*(nFiles+1)/2)%nFiles;
		
		return newId;
	}
		
	
	public void updateEdges() {
		g.edges = new ArrayList<Edge>();
		g.edgesFrom = new HashMap<Node, ArrayList<Edge>>();
		g.edgesTo = new HashMap<Node, ArrayList<Edge>>();
		
		// Update slider value to synchronize the processes
		System.out.println();
		for (int r = 0; r < Graph.nodes.size(); r++) {
			Node node1 = Graph.nodes.get(r);
			ArrayList<Integer> a = getDirectDownstream(r);
			for (int j = 0; j < a.size(); j++) {
				int r2 = a.get(j);
				Node node2 = Graph.nodes.get(r2);
				Edge e = new Edge(node1, node2, 0, parent); //
				g.addEdge(e);
				node1.degree++;
			}
			/*
			ArrayList<Integer> b = getReactionWithSameInput(r);
			for (int j = 0; j < b.size(); j++) {
				int r2 = b.get(j);
				Node node2 = g.nodes.get(r2);
				Edge e = new Edge(node1, node2, 1, parent);  // Same input
				g.addEdge(e);
				node1.degree++;
			}
			ArrayList<Integer> c = getReactionWithSameOutput(r);
			for (int j = 0; j < c.size(); j++) {
				int r2 = c.get(j);
				Node node2 = g.nodes.get(r2);
				Edge e = new Edge(node1, node2, 2, parent);  // Same output
				g.addEdge(e);
				node1.degree++;
			}*/
		}	
	}
	
	public ArrayList<Integer> getProteinsInOneSideOfReaction(Object[] s) {
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int i3=0;i3<s.length;i3++){
			  String ref = s[i3].toString();
			  if (mapProteinRDFId.get(ref)!=null){
				  String proteinName = mapProteinRDFId.get(ref);
				  int index = proteinList.indexOf(proteinName);
				  a.add(index);
			  }
			  else  if (mapComplexRDFId.get(ref)!=null){
				  ArrayList<String> components = getProteinsInComplexRDFId(ref);
				  for (int k=0;k<components.size();k++){
					  String proteinName = mapComplexRDFId.get(components.get(k));
					  int index = proteinList.indexOf(proteinName);
					  a.add(index);
				  }
			  }
			  else{
				  System.out.println("getProteinsInOneSideOfReaction: CAN NOT FIND ="+s[i3]+"-----SOMETHING WRONG");
			 } 
		  }
		return a;
	}
	
	public ArrayList<String> getProteinsInComplexRDFId(String ref){	
		ArrayList<String> components = new ArrayList<String>(); 
		Complex com = mapComplexRDFId_Complex.get(ref);
		Object[] s2 = com.getComponent().toArray();
		for (int i=0;i<s2.length;i++){
			String ref2 = s2[i].toString();
			 if (mapProteinRDFId.get(ref2)!=null)
				  components.add(mapProteinRDFId.get(ref2));
			 else if (mapComplexRDFId.get(ref2)!=null){
				  ArrayList<String> s4 = getProteinsInComplexRDFId(ref2);
				  for (int k=0;k<s4.size();k++){
					  components.add(s4.get(k));
				  }
			  }
		 }
		 return components;
	}
	
	public void draw(){
		if (!isAllowedDrawing || g==null || g.nodes==null) return;
		xRight = parent.width*7.5f/10;
		
		for (int i=0;i<g.nodes.size();i++){
			Node node = g.nodes.get(i);
			node.iX.update();
			node.iY.update();
			node.iAlpha.update();
			
		}
		
		if (popupLayout.s==0){
			drawTree();
			
			iTransition.target(PApplet.PI);
			iTransition.update();
			g.drawNodes();
			g.drawEdges();
		}
		else if (popupLayout.s==1){
			iTransition.target(PApplet.PI);
			iTransition.update();
			g.drawNodes();
			g.drawEdges();
		}
		else if (popupLayout.s==2){
			iTransition.target(0);
			iTransition.update();
			if (g==null) return;
			doLayout();
			g.drawEdges();
			g.drawNodes();
		}
		else if (popupLayout.s==3){
			iTransition.target(1);
			iTransition.update();
			
			float totalSize=0;
			for (int i=0;i<filePathway.length;i++){
				totalSize += PApplet.sqrt(filePathway[i].numReactions);
			}
			
			if (rootPathway.isExpanded){
				float currentPos=0;
				bPathway = null;
				for (int i=0;i<filePathway.length;i++){
					float newPos = currentPos+PApplet.sqrt(filePathway[i].numReactions)/2;
					float al = (newPos/totalSize)*2*PApplet.PI - PApplet.PI/2;
					float xR2 = PathwayView.xCircular + (PathwayView.rCircular+filePathway[i].radiusCenter)*PApplet.cos(al);
					float yR2 = PathwayView.yCircular + (PathwayView.rCircular+filePathway[i].radiusCenter)*PApplet.sin(al);
					filePathway[i].draw(xR2, yR2,al);
					currentPos += PApplet.sqrt(filePathway[i].numReactions);
				}
			}
			else{
				// Print all reactions on a circle
				
				float beginAl = -PApplet.PI/2;
				for (int f=0;f<filePathway.length;f++){
					ArrayList<Integer> a = filePathway[f].getAllNodeId();
					float sec = (PApplet.sqrt(a.size())/totalSize)*PApplet.PI*1.8f;
					for (int i=0;i<a.size();i++){
						Node node =Graph.nodes.get(a.get(i));
						float al2 = beginAl+((float) i/a.size())*sec;
						if (node==null) return;
						float xR2 = xCircular + (rCircular+node.size/2)*PApplet.cos(al2);
						float yR2 = yCircular + (rCircular+node.size/2)*PApplet.sin(al2);
						Pathway2.setNodePosistion(node, xR2,yR2,al2);	
					}
					beginAl += sec + (PApplet.PI*0.2f)/filePathway.length;
				}
			}
			
			isBrushing =false;
			float rCenter = rCircular/10;
			rootPathway.x = xCircular;
			rootPathway.y = yCircular;
			if (PApplet.dist(xCircular, yCircular, parent.mouseX, parent.mouseY)<rCenter){
				isBrushing = true;
			}
				
			parent.noStroke();
			parent.fill(Pathway2.beginDarknessOfPathways);  
			if (isBrushing)
				parent.fill(220,220,255,200);
			parent.ellipse(xCircular, yCircular, rCircular*2, rCircular*2);
			
			drawCenter(xCircular, yCircular,rCenter);
			
			// Draw brushing pathway
			if (bPathway!=null)
				bPathway.drawWhenBrushing();
			
			g.drawNodes();
		   	g.drawEdges();
		   	
		   	// Draw button to control the map
		   	buttonReset.draw("Reset map",xRight-buttonReset.w-3, 2);
		   	buttonExpand.draw("Expand all",xRight-buttonExpand.w-3, 24);
		   	buttonCollapse.draw("Collapse all",xRight-buttonCollapse.w-3, 46);
		   	
		}
		
		// Right PANEL
		float wRight = parent.width-xRight;
		parent.fill(200,200);
		parent.noStroke();
		parent.rect(xRight, 25, wRight, parent.height-25);
		slider2.draw("Edge length",xRight+100, 50);
		checkName.draw(xRight+30, 80);
		// File names
		parent.textSize(12);
		parent.textAlign(PApplet.LEFT);
		for (int f=0; f<nFiles; f++){
			float yy = 200+f*18;
			String[] str = files.get(f).split("/");
			String nameFile = str[str.length-1];
			Color color = gradient.getGradient(colorScale*(transferID(f)));
			parent.fill(color.getRGB());
			parent.text(nameFile, xRight+20,yy); 
		}	
		// Draw popups
		popupLayout.draw(parent.width-198);
		popupPathway.draw(parent.width-298);
		setIntegrator --;  // Set node to circular layout when dragging or changing scales
		
		
		// Draw brushing reaction
		if (g.getHoverNode()!=null){
			System.out.println("getHoverNode	"+g.getHoverNode().reaction.getDisplayName());
			Node node =g.getHoverNode();
			drawReactionLink(parent, node, xRight, parent.width-80, 500, 255);
		}
		
	}
	
	
	// draw Reactions links
	public static void drawReactionLink(PApplet parent, Node node, float xL, float xR,float yReact, float sat) {
		
		Object[] sLeft = node.reaction.getLeft().toArray();
		for (int i3=0;i3<sLeft.length;i3++){
			  String name = main.PathwayViewer_2_6.getProteinName(sLeft[i3].toString());
			  if (name==null)
				  name=sLeft[i3].toString();
			  
			  float y2 = yReact+i3*20;
			  parent.stroke(0);
			  parent.line(xL, y2, (xL+xR)/2, yReact);
			
			  parent.fill(0);
			  parent.textSize(11);
			  parent.textAlign(PApplet.RIGHT);
			  parent.text(name,xL, y2+5);
			  
			 /* float al = -PApplet.PI/6;
			   parent.translate(x2,yL);
				parent.rotate(al);
				parent.text(name, 0,0);
				parent.rotate(-al);
				parent.translate(-(x2), -(yL));
			*/
			  /*
			  // Complex LEFT
			  else if (main.PathwayViewer_2_5.mapComplexRDFId_index.get(sLeft[i3].toString())!=null){
				  int id = main.PathwayViewer_2_5.mapComplexRDFId_index.get(sLeft[i3].toString());
				 // isContainedComplexL = drawComplexLeft(i2, id, yReact, sat);
			  }
			  else{
				//System.out.println("drawReactionLink Left: CAN NOT FIND ="+sLeft[i3]);
			  }*/
		  }
		Object[] sRight = node.reaction.getRight().toArray();
		for (int i3=0;i3<sRight.length;i3++){
			  String name = main.PathwayViewer_2_6.getProteinName(sRight[i3].toString());
			  if (name==null)
				  name=sRight[i3].toString();
			  
			  float y2 = yReact+i3*20;
			  parent.stroke(0);
			  parent.line( xR, y2, (xL+xR)/2, yReact);
			
			  parent.fill(0);
			  parent.textSize(11);
			  parent.textAlign(PApplet.LEFT);
			  parent.text(name,xR, y2+5);
			  
			  /*
			  float al = PApplet.PI/6;
			   parent.translate(x2,yR);
				parent.rotate(al);
				parent.text(name, 0,0);
				parent.rotate(-al);
				parent.translate(-(x2), -(yR));
			  */	
				
			  /*
			  // Complex LEFT
			  else if (main.PathwayViewer_2_5.mapComplexRDFId_index.get(sLeft[i3].toString())!=null){
				  int id = main.PathwayViewer_2_5.mapComplexRDFId_index.get(sLeft[i3].toString());
				 // isContainedComplexL = drawComplexLeft(i2, id, yReact, sat);
			  }
			  else{
				//System.out.println("drawReactionLink Left: CAN NOT FIND ="+sLeft[i3]);
			  }*/
		  }
		
		parent.fill(node.color.getRGB());
		parent.noStroke();
		parent.ellipse((xL+xR)/2, yReact, node.size, node.size);
		parent.textAlign(PApplet.CENTER);
		parent.text(node.reaction.getDisplayName(), (xL+xR)/2, yReact-node.size/2-2);
	 }
	
	
	
	 public void drawCenter(float x_, float y_, float r_){
		parent.fill(50);
	  	parent.ellipse(x_, y_, r_*2, r_*2);
		
	  	parent.strokeWeight(r_/10);
		parent.stroke(150);
		parent.line(x_-(r_*1.6f)/2,y_, x_+(r_*1.6f)/2,y_);
		if (!rootPathway.isExpanded)
			parent.line(x_,y_-(r_*1.6f)/2, x_,y_+(r_*1.6f)/2);
	}
		
	
	public void drawTree() {
		parent.fill(0);
		parent.textSize(12);
		parent.textAlign(PApplet.LEFT);
		parent.text(nFiles+" files", 3, parent.height/2+5);
		float[] yF = new float[nFiles];
		float[] nF = new float[nFiles];
		for (int i=0; i<Graph.nodes.size(); i++){
			int f = rectFileList.get(i);
			yF[f] += g.nodes.get(i).iY.value;
			nF[f] ++;
		}
		parent.stroke(0);
		parent.strokeWeight(1);
		parent.textSize(11);
		for (int f=0; f<nFiles; f++){
			float yy = yF[f]/nF[f];
			parent.line(40,parent.height/2,250,yy);
			
		}
		for (int i=0; i<Graph.nodes.size(); i++){
			int f = rectFileList.get(i);
			float yy = yF[f]/nF[f];
			Color color = gradient.getGradient(colorScale*(transferID(f)));
			parent.stroke(color.getRed(), color.getGreen(), color.getBlue(),60);
			parent.line(250,yy, g.nodes.get(i).iX.value,g.nodes.get(i).iY.value);
		}
			
		for (int f=0; f<nFiles; f++){
			float yy = yF[f]/nF[f];
			String[] str = files.get(f).split("/");
			String nameFile = str[str.length-1];
			Color color = gradient.getGradient(colorScale*(transferID(f)));
			parent.fill(color.getRGB());
			parent.text(nameFile, 250,yy); 
		}
	
	}
		
	public void orderTopological() {
		ArrayList<Integer> doneList = new ArrayList<Integer>();
		ArrayList<Integer> circleList = new ArrayList<Integer>();
		
		int count = 0;
		int r = getNoneUpstream(doneList);
		while (count<Graph.nodes.size()){
		//	System.out.println(count+"	doneList="+doneList+"	r="+r);
			if (r>=0){
				doneList.add(r);
				r = getNoneUpstream(doneList);
			}
			else{
				int randomReaction = getReactionMaxDownstream(doneList);
				doneList.add(randomReaction);
				circleList.add(randomReaction);
				r = getNoneUpstream(doneList);
			}	
			count++;
		}
		
		
		// Compute nonCausality reaction
		ArrayList<Integer> nonCausalityList = new ArrayList<Integer>();
		for (int i=0;i<doneList.size();i++){
			int index = doneList.get(i);
			if (getDirectUpstream(index).size()==0 && getDirectDownstream(index).size()==0)
				nonCausalityList.add(index);
		}
		
		float totalH = parent.height-15;
		float itemH2 = totalH/(Graph.nodes.size()+circleList.size()-nonCausalityList.size()*0.8f+1);
		float circleGap = itemH2;
		float circleGapSum = 0;
		
		int count2 = 0;
		int count3 = 0;
		float yStartCausality = 10 +(Graph.nodes.size()-nonCausalityList.size()+circleList.size()+1)*itemH2;
		for (int i=0;i<doneList.size();i++){
			int index = doneList.get(i);
			// Compute nonCausality reaction
			if (getDirectUpstream(index).size()==0 && getDirectDownstream(index).size()==0){
				yTopological[index] =  yStartCausality +count3*itemH2*0.2f;
				count3++;
			}	
			else{
				if(circleList.contains(index)){
					yTopological[index] = circleGapSum+ 10+count2*itemH2+circleGap;
					circleGapSum +=circleGap;
				}
				else{
					yTopological[index] = circleGapSum+10+count2*itemH2;
				}
				count2++;
			}
		}
	}
	
	public void orderTree() {
		yTree =  new float[Graph.nodes.size()];
		for (int i=0; i<Graph.nodes.size();i++){
			float totalH = parent.height-10;
			float itemH2 = (totalH-10*nFiles)/(Graph.nodes.size()-1);
			yTree[i] = 10+i*itemH2+10*rectFileList.get(i);
		}
	}
		
	public int getNoneUpstream(ArrayList<Integer> doneList){
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int i=0;i<Graph.nodes.size();i++){
			if (doneList.contains(i)) continue;
			ArrayList<Integer> up = this.getDirectUpstream(i);
			if (up.size()==0)  {//No upstream
				a.add(i);
			}	
		}
		if (a.size()>0){
			return getReactionMinDownstreamIn(a);
		}
		else{
			ArrayList<Integer> b = new ArrayList<Integer>();
			for (int i=0;i<Graph.nodes.size();i++){
				if (doneList.contains(i)) continue;
				ArrayList<Integer> up = this.getDirectUpstream(i);
				if (isContainedAllUpInDoneList(up,doneList)){  // Upstream are all in the doneList;
				//	return i;
					b.add(i);
				}	
			}
			if (b.size()>0){
				return getReactionMaxUpstreamIn(b);
			}
			return -1;
		}
	}
	public boolean isContainedAllUpInDoneList(ArrayList<Integer> up, ArrayList<Integer> doneList){
		for (int i=0;i<up.size();i++){
			int r = up.get(i);
			if (!doneList.contains(r))
				return false;
		}
		return true;
	}
	
	public int getReactionMaxUpstreamIn(ArrayList<Integer> list){
		int numUpstream = 0;
		int react = -1;
		for (int i=0;i<list.size();i++){
			int index = list.get(i);
			ArrayList<Integer> up = getDirectUpstream(index);
			if (up.size()>=numUpstream){
				numUpstream = up.size();
				react =index;
			}	
		}
		return react;
	}
	
	public int getReactionMaxDownstream(ArrayList<Integer> doneList){
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int i=0;i<Graph.nodes.size();i++){
			if (doneList.contains(i)) continue;
			a.add(i);
		}
		return getReactionMaxDownstreamIn(a);
	}
	
	public int getReactionMaxDownstreamIn(ArrayList<Integer> list){
		int numDownstream = 0;
		int react = -1;
		for (int i=0;i<list.size();i++){
			int index = list.get(i);
			ArrayList<Integer> down = getDirectDownstream(index);
			if (down.size()>=numDownstream){
				numDownstream = down.size();
				react =index;
			}	
		}
		return react;
	}
	
	public int getReactionMinDownstreamIn(ArrayList<Integer> list){
		int numDownstream = Integer.MAX_VALUE;
		int react = -1;
		for (int i=0;i<list.size();i++){
			int index = list.get(i);
			ArrayList<Integer> down = getDirectDownstream(index);
			if (down.size()<numDownstream){
				numDownstream = down.size();
				react =index;
			}	
		}
		return react;
	}
	
	public ArrayList<Integer> getReactionWithSameInput(int r){
		ArrayList<Integer> a = new ArrayList<Integer>();
		BiochemicalReaction rectSelected = Graph.nodes.get(r).reaction;
		Object[] sLeft1 = rectSelected.getLeft().toArray();
		for (int g=0;g<Graph.nodes.size();g++) {
			if(g==r) continue;
			BiochemicalReaction rect2 = Graph.nodes.get(g).reaction;
			Object[] sLeft2 = rect2.getLeft().toArray();
			ArrayList<String> commonElements = compareInputOutput(sLeft1, sLeft2);
			if (commonElements.size()>0){
				a.add(g);
			}
		}
		return a;
	}
	public ArrayList<Integer> getReactionWithSameOutput(int r){
		ArrayList<Integer> a = new ArrayList<Integer>();
		BiochemicalReaction rectSelected = Graph.nodes.get(r).reaction;
		Object[] sRight1 = rectSelected.getRight().toArray();
		for (int g=0;g<Graph.nodes.size();g++) {
			if(g==r) continue;
			BiochemicalReaction rect2 = Graph.nodes.get(g).reaction;
			Object[] sRight2 = rect2.getRight().toArray();
			ArrayList<String> commonElements = compareInputOutput(sRight1, sRight2);
			if (commonElements.size()>0){
				a.add(g);
			}
		}
		return a;
	}
	
	
	public ArrayList<Integer> getDirectDownstream(int r){
		ArrayList<Integer> a = new ArrayList<Integer>();
		BiochemicalReaction rectSelected = Graph.nodes.get(r).reaction;
		Object[] sRight1 = rectSelected.getRight().toArray();
		for (int g=0;g<Graph.nodes.size();g++) {
			if(g==r) continue;
			BiochemicalReaction rect2 = Graph.nodes.get(g).reaction;
			Object[] sLeft2 = rect2.getLeft().toArray();
			ArrayList<String> commonElements = compareInputOutput(sRight1, sLeft2);
			if (commonElements.size()>0){
				a.add(g);
			}
		}
		return a;
	}
	
	
	public ArrayList<Integer> getDirectUpstream(int r){
		ArrayList<Integer> a = new ArrayList<Integer>();
		BiochemicalReaction rectSelected = Graph.nodes.get(r).reaction;
		Object[] sLeft = rectSelected.getLeft().toArray();
		
		// List current reaction
		for (int g=0;g<Graph.nodes.size();g++) {
			if(g==r) continue;
			BiochemicalReaction rect2 = Graph.nodes.get(g).reaction;
			Object[] sRight2 = rect2.getRight().toArray();
			ArrayList<String> commonElements = compareInputOutput(sRight2, sLeft);
			if (commonElements.size()>0){
				a.add(g);
			}
		}
		return a;
	}
	
	public void doLayout() {
		// calculate forces on each node
		// calculate spring forces on each node
		for (int i = 0; i < g.getNodes().size(); i++) {
			Node n = (Node) g.getNodes().get(i);
			ArrayList edges = (ArrayList) g.getEdgesFrom(n);
			n.setForce(new Vector3D(0, 0, 0));
			for (int j = 0; edges != null && j < edges.size(); j++) {
				Edge e = (Edge) edges.get(j);
				Vector3D f = e.getForceFrom();
				n.applyForce(f);
			}
			edges = (ArrayList) g.getEdgesTo(n);
			for (int j = 0; edges != null && j < edges.size(); j++) {
				Edge e = (Edge) edges.get(j);
				Vector3D f = e.getForceTo();
				n.applyForce(f);
			}
		}
		

		// calculate the anti-gravitational forces on each node
		// this is the N^2 shittiness that needs to be optimized
		// TODO: at least make it N^2/2 since forces are symmetrical
		for (int i = 0; i < g.getNodes().size(); i++)  {
			Node a = (Node) g.getNodes().get(i);
			for (int j = 0; j < g.getNodes().size(); j++) {
				Node b = (Node) g.getNodes().get(j);
				if (b != a) {
					float dx = b.getX() - a.getX();
					float dy = b.getY() - a.getY();
					float r = PApplet.sqrt(dx * dx + dy * dy);
					// F = G*m1*m2/r^2

					float f = 5*(a.getMass() * b.getMass() / (r * r));
					if (a.degree>0){
						f = PApplet.sqrt(a.degree)*f;
					}
					if (r > 0) { // don't divide by zero.
						Vector3D vf = new Vector3D(-dx * f, -dy * f, 0);
						a.applyForce(vf);
					}
				}
			}
		}
		
		float xCenter = xRight/2;
		float yCenter = parent.height/2;
		for (int i = 0; i < g.getNodes().size(); i++) {
			Node a = (Node) g.getNodes().get(i);
			float dx = xCenter - a.getX();
			float dy = yCenter - a.getY();
			float r2 = dx * dx + dy * dy;
			float f =  r2/10000000;
			if (a.degree>0){
				Vector3D vf = new Vector3D(dx * f, dy * f, 0);
				a.applyForce(vf);
			}
		}

		// move nodes according to forces
		for (int i = 0; i < g.getNodes().size(); i++) {
			Node n = (Node) g.getNodes().get(i);
			if (n != g.getDragNode()) {
				n.setPosition(n.getPosition().add(n.getForce()));
			}
		}
	}
	
	
	public static float computeAlpha(int r){
		return PApplet.PI -((float)r)/(Graph.nodes.size())*2*PApplet.PI;
	}
	
			
	
	public ArrayList<String> compareInputOutput(Object[] a, Object[] b){
		ArrayList<String> results = new ArrayList<String>();
		for (int i=0; i<a.length;i++){
			String ref1 = a[i].toString();
			if (mapProteinRDFId.get(ref1)!=null){
				String proteinName1 = mapProteinRDFId.get(ref1);
				for (int j=0; j<b.length;j++){
					String ref2 = b[j].toString();
					String proteinName2 = mapProteinRDFId.get(ref2);
					if (proteinName2!=null && proteinName1.equals(proteinName2) 
							&& !mapSmallMoleculeRDFId.containsValue(proteinName1)){
							 results.add(proteinName1);
					}	
				}
			}
			else if (mapComplexRDFId.get(ref1)!=null){
				String complexName1 = mapComplexRDFId.get(ref1);
				for (int j=0; j<b.length;j++){
					String ref2 = b[j].toString();
					String complexName2 = mapComplexRDFId.get(ref2);
					if (complexName2!=null && complexName1.equals(complexName2)){
						 results.add(complexName1);
					}	
				}
			}
			
		}
		return results;
	}
	
	
	
	

	
	
	public void keyPressed() {
		if (g!=null){
			ArrayList<Node> nodes= g.getNodes();
			/*if (parent.key == '+') {
				//g.removeNode(g.getNodes().get(1));
				return;
			} else if (parent.key == '-') {
				g.removeNode(nodes.get(4));
				return;
			}*/
		}
	}

	public void mousePressed() {
		if (g==null) return;
		g.setDragNode(null);
		slider2.checkSelectedSlider1();
		for (int i = 0; i < g.getNodes().size(); i++) {
			Node n = (Node) g.getNodes().get(i);
			if (n.containsNode(parent.mouseX, parent.mouseY)) {
				g.setDragNode(n);
			}
		}
	}
	
	public void mouseReleased() {
		if (g==null) return;
		g.setDragNode(null);
		slider2.checkSelectedSlider2();
	}

	public void mouseMoved() {
		if (g!=null && g.getDragNode() == null) {
			g.setHoverNode(null);
			for (int i = 0; i < g.getNodes().size(); i++) {
				Node n = (Node) g.getNodes().get(i);
				if (n.containsNode(parent.mouseX, parent.mouseY)) {
					g.setHoverNode(n);
				}
			}
		}
		popupLayout.mouseMoved();
	}
	
	public void mouseClicked() {
		if (g==null) return;
		
		if(isBrushing){
			rootPathway.isExpanded = !rootPathway.isExpanded;
			if (!rootPathway.isExpanded)
				rootPathway.collapseAll();   // When we close a pathway, close all sub-pathway recursively
		}
		else if (bPathway!=null){
			bPathway.isExpanded=!bPathway.isExpanded;
			if (!bPathway.isExpanded)
				bPathway.collapseAll();   // When we close a pathway, close all sub-pathway recursively
		}
		else if (buttonExpand.b){
			rootPathway.expandAll();
		}
		else if (buttonCollapse.b){
			rootPathway.collapseAll();
		}
		else if (buttonReset.b){
			resetPosistion();
			scale = 1f;
			updateScale();
			setIntegrator = 4;
		}
			
		
		if (popupLayout.b>=0){
			popupLayout.mouseClicked();

			if (popupLayout.s==0){
				orderTree();
			}
			else if (popupLayout.s==1){
				iTransition.target(PApplet.PI);
				thread5 = new Thread(loader5);
				thread5.start();
			}
			else if (popupLayout.s==2)
				iTransition.target(0);
			else  if (popupLayout.s==3){
				iTransition.target(1);
			}
			
			
		}
		else if(popupPathway.bPopup){
			popupPathway.mouseClicked();
		}
		else if (checkName.b)
			checkName.mouseClicked();
		else{
			g.setSelectedNode(null);
			for (int i = 0; i < g.getNodes().size(); i++) {
				Node n = (Node) g.getNodes().get(i);
				if (n.containsNode(parent.mouseX, parent.mouseY)) {
					g.setSelectedNode(n);
				}
			}
		}
	}

	public void mouseDragged() {
		slider2.checkSelectedSlider3();
		setIntegrator = 2;
		if (g==null) return;
		if (g.getDragNode() != null) {
			g.getDragNode()
					.setPosition(
							new Vector3D(parent.mouseX, parent.mouseY, 0));
		}
		else{
			xCircular += (parent.mouseX - parent.pmouseX)*PathwayView.scale;
			yCircular += (parent.mouseY - parent.pmouseY)*PathwayView.scale;
		}
	}
	

	// Thread for grouping
	class ThreadLoader5 implements Runnable {
		PApplet parent;
		public ThreadLoader5(PApplet p) {
			parent = p;
		}
		public void run() {
			orderTopological();
		}
	}	
	
}
	