package main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.biopax.paxtools.model.level3.BiochemicalReaction;

import processing.core.PApplet;

import GraphLayout.Graph;
import GraphLayout.Node;


public class Pathway2{
  public Pathway2 parentPathway;
  public ArrayList<Pathway2> subPathwayList;
  public ArrayList<BiochemicalReaction> reactList;
  public ArrayList<Integer> nodeIdList;
  public int f = -1; 
  public int level = -1; 
  public String displayName = "?";
  
  // for drawing
  public float radius = 0;
  public float radiusCenter = 0;
  public int numReactions = 0;
  public boolean isExpanded = false;
  
  public float x = 100;
  public float y = 100;
  public float xEntry = 500;
  public float yEntry = 500;
  public float al = 0;
  private PApplet parent = null;
  public static float beginDarknessOfPathways = 150;
  
  // Draw the button threading
  public int linkToParent = 0;
  public int linkFromParent = 0;
   
  
  // Constructor
  Pathway2(PApplet parent_, Pathway2 parentPathway_, int f_, String dName, int level_, boolean isExpande_){
	  parent = parent_;
	  parentPathway = parentPathway_;
	  f=f_;
	  level = level_;
	  displayName = dName;
	  isExpanded = isExpande_;
	  subPathwayList =  new ArrayList<Pathway2>();
	  reactList = new ArrayList<BiochemicalReaction>();
	  nodeIdList = new ArrayList<Integer>();
  }
  
  public int computeSize(){
	  numReactions = reactList.size();
	  for (int i=0; i<subPathwayList.size();i++){
		  numReactions+=subPathwayList.get(i).computeSize();
	  }
	   return numReactions;
  }
  
  public void expandAll(){
	  isExpanded = true;
	  for (int p=0;p<subPathwayList.size();p++){
		  subPathwayList.get(p).expandAll();
	  }
  }
  public void collapseAll(){
	  isExpanded = false;
	  for (int p=0;p<subPathwayList.size();p++){
		  subPathwayList.get(p).collapseAll();
	  }
  }
		
		
  public void draw(float x_, float y_, float xEntry_, float yEntry_, float al_){
	  x=x_;
	  y=y_;
	  xEntry = xEntry_;
	  yEntry = yEntry_;
	  
	  al = al_;
	  radius = PApplet.pow(numReactions,0.6f)*PathwayView.scale*5;
	  radiusCenter = radius/4;
	  parent.noStroke();
	  	if (isExpanded)
	  		drawExpanded();
	  	else 
	  		drawUnexpanded();
	  	
		if (PathwayView.bPathway==null && PApplet.dist(x, y, parent.mouseX, parent.mouseY)<radius){
			PathwayView.bPathway = this;
		}
		else{
			float v = beginDarknessOfPathways+(level+1)*14;  
		  	if (v>240)
		  		v=240;
		  
			parent.fill(v);
		  	parent.noStroke();
			parent.arc(x, y, radius*2, radius*2,al-PApplet.PI/2,al-PApplet.PI/2+PApplet.PI*2);
			drawCenter();
		}
	}	
  public void drawWhenBrushing(){
	  parent.fill(255,220,255,230);
	  parent.noStroke();
	  parent.ellipse(x, y, radius*2, radius*2);
	  parent.fill(0);
	  parent.textAlign(PApplet.CENTER);
	  parent.textSize(12);
	  parent.text(displayName,x,y-10);
	  drawCenter();
  }
  
  //Draw center
	public void drawCenter(){
		Color color2 = PathwayView.getColor(f).darker().darker();
		float sat = 200+level*10;
	  	if (sat>255)
	  		sat=255;
	  	parent.fill(color2.getRed(),color2.getGreen(),color2.getBlue(),sat);
	  	float xCenter = x-radiusCenter/2*PApplet.cos(al);
	  	float yCenter = y-radiusCenter/2*PApplet.sin(al);
	  	parent.ellipse(xCenter, yCenter, radiusCenter, radiusCenter);
		
	  	parent.strokeWeight(radiusCenter/20);
		parent.stroke(150);
		parent.line(xCenter-(radiusCenter*0.8f)/2,yCenter, xCenter+(radiusCenter*0.8f)/2,yCenter);
		if (!isExpanded)
			parent.line(xCenter,yCenter-(radiusCenter*0.8f)/2, xCenter,yCenter+(radiusCenter*0.8f)/2);
		
		// draw file pathway name in the first level
		if (level==1 && !isExpanded){
			if (-PApplet.PI/2<=al && al<PApplet.PI/2){
				parent.textAlign(PApplet.LEFT);
				parent.translate(x,y);
				parent.rotate(al);
				parent.fill(255);
				parent.text(displayName, 4, 5);
				parent.fill(PathwayView.getColor(f).getRGB());
				parent.text(displayName, 3, 4);
				parent.rotate(-al);
				parent.translate(-x,-y);
			}
			else{
				parent.textAlign(PApplet.RIGHT);
				parent.translate(x,y);
				parent.rotate(al-PApplet.PI);
				parent.fill(255);
				parent.text(displayName, -2, 4);
				parent.fill(PathwayView.getColor(f).getRGB());
				parent.text(displayName, -3, 3);
				parent.rotate(-(al-PApplet.PI));
				parent.translate(-x,-y);
			}
		}
		
  }
		
		
  
  public void drawUnexpanded(){
	  ArrayList<Integer> a = getAllNodeId();
	  float numNode = a.size();  // Number of points on the circles including reactions and pathways 
	  
	  // Draw reactions
	  for (int i=0; i<a.size();i++){
		  Node node = Graph.nodes.get(a.get(i));
		  float al2 = al+PApplet.PI*0.55f -(i+1f)/(numNode+1f)*PApplet.PI*1.1f;  // Right
		  //System.out.println("node="+node);
		  if (node==null) return;
		  float xR2 = x + (radius+node.size/2)*PApplet.cos(al2);
		  float yR2 = y + (radius+node.size/2)*PApplet.sin(al2);
		  setNodePosistion(node, xR2,yR2,al2);
	  }
  }
  
  // Does not check redundant reactions
  public ArrayList<BiochemicalReaction> getAllReaction(){
	  ArrayList<BiochemicalReaction> a =  new ArrayList<BiochemicalReaction>();
	  for (int i=0; i<reactList.size();i++){
		  a.add(reactList.get(i));
	  }
	  for (int p=0;p<subPathwayList.size();p++){
		  ArrayList<BiochemicalReaction> b = subPathwayList.get(p).getAllReaction();
		  for (int i=0;i<b.size();i++){
			  a.add(b.get(i));
		  }
	  }
	  return a;
  }	
  
  // Does not check redundant reactions
  public ArrayList<Integer> getAllNodeId(){
	  ArrayList<Integer> a =  new ArrayList<Integer>();
	  for (int i=0; i<nodeIdList.size();i++){
		  a.add(nodeIdList.get(i));
	  }
	  for (int p=0;p<subPathwayList.size();p++){
		  ArrayList<Integer> b = subPathwayList.get(p).getAllNodeId();
		  for (int i=0;i<b.size();i++){
			  a.add(b.get(i));
		  }
	  }
	  return a;
  }	
  
  public void drawExpanded(){
		  float numSect = nodeIdList.size();  // Number of points on the circles including reactions and pathways 
		  for (int i=0; i<subPathwayList.size();i++){
			  Pathway2 pathway = subPathwayList.get(i);
			   numSect += pathway.numReactions/2;
		  }
		  // Draw reactions
		  int countReactionLeft = 0;
		  int countReactionRight = 0;
		  float leftAl = al-PApplet.PI*0.55f;
		  float rightAl = al+PApplet.PI*0.55f;
		  for (int i=0; i<nodeIdList.size();i++){
			  Node node = Graph.nodes.get(nodeIdList.get(i));
			  float al2=0;
			  if (i%2==0){
				  al2 = al+PApplet.PI*0.55f -(countReactionRight+1f)/(numSect+1f)*PApplet.PI*1.1f;  // Right
				  rightAl = al2;
				  countReactionRight++;
			  }
			  else{
			  	  al2 = al-PApplet.PI*0.55f +(countReactionLeft+1f)/(numSect+1f)*PApplet.PI*1.1f;
			  	  leftAl = al2;
				  countReactionLeft++;
			  }
			  //System.out.println("node="+node);
			  if (node==null) return;
			  float xR2 = x + (radius+node.size/2)*PApplet.cos(al2);
			  float yR2 = y + (radius+node.size/2)*PApplet.sin(al2);
			  setNodePosistion(node, xR2,yR2,al2);
		  }
		  
		  // Draw subpathway
  		  float total = 0;
		  float dif =rightAl-leftAl;
		  for (int i=0; i<subPathwayList.size();i++){
			  Pathway2 pathway = subPathwayList.get(i);
			  total+=PApplet.sqrt(pathway.numReactions);
		  }
		  float sum = 0;
		  for (int i=0; i<subPathwayList.size();i++){
			  Pathway2 pathway = subPathwayList.get(i);
			  
			  float percent = (sum+PApplet.sqrt(pathway.numReactions)/2)/total;
			  float al = leftAl +percent*dif;
			  float xR2 = x + (radius+pathway.radiusCenter)*PApplet.cos(al);
			  float yR2 = y + (radius+pathway.radiusCenter)*PApplet.sin(al);
			  float xR3 = x + (radius)*PApplet.cos(al);
			  float yR3 = y + (radius)*PApplet.sin(al);
			  pathway.draw(xR2, yR2, xR3, yR3, al);
			  sum+=PApplet.sqrt(pathway.numReactions);
			  //current+=pathway.numReactions/2;
		  }
  }
  
  public static void setNodePosistion(Node node, float xR2, float yR2, float al2){
	  node.iAlpha.target(al2);
	  node.iX.target(xR2);
	  node.iY.target(yR2);
	  if (PathwayView.setIntegrator>0){
		  node.iAlpha.set(al2);
		  node.iX.set(xR2);
		  node.iY.set(yR2);
	  }
  }
  

	// Every reaction associated to a node id in the nodes of Graph class
	public int setNodeId(int nodeId) {
		int newId = nodeId;
		for (int i=0;i<reactList.size();i++){
			nodeIdList.add(newId);
			newId++;
		}
		for (int i=0;i<subPathwayList.size();i++){
			Pathway2 subpathway = subPathwayList.get(i);
			newId = subpathway.setNodeId(newId);
		}
		return newId;
	}
	
	
	// Every reaction associated to this pathway should contain this pathway as parent pathway
	public void setNodePathway() {
		for (int i=0;i<nodeIdList.size();i++){
			Graph.nodes.get(nodeIdList.get(i)).parentPathway= this;
		}
		for (int i=0;i<subPathwayList.size();i++){
			Pathway2 subpathway = subPathwayList.get(i);
			subpathway.setNodePathway();
		}
	}
	
			
  public boolean isContainReaction(String rName){
	  for (int r=0;r<reactList.size();r++){
		  String name = reactList.get(r).getDisplayName();
		  if (name==null) continue;
 		  if (name.equals(rName))
			  return true;
	  }
	  for (int p=0;p<subPathwayList.size();p++){
		  Pathway2 path = subPathwayList.get(p);
		  boolean result = path.isContainReaction(rName);
		  if (result)
			  return true;
	  }
	  return false;
  }
  
  public boolean isContainPathway(String pName){
	  //if (pName.equals(displayName))
	  //	  return true;
	  for (int p=0;p<subPathwayList.size();p++){
		  Pathway2 path = subPathwayList.get(p);
		  if (path.displayName.equals(pName) || path.isContainPathway(pName))
			  return true;
	  }
	  return false;
  }
  
  public ArrayList<Pathway2> printRecursively(){
	  ArrayList<Pathway2> a = new ArrayList<Pathway2>();
	  a.add(this);	
	  
	  for (int p=0;p<subPathwayList.size();p++){
		  ArrayList<Pathway2> b = subPathwayList.get(p).printRecursively();
		  for (int i=0;i<b.size();i++){
			  a.add(b.get(i));
		  }
	  }
	  return a;
  }
  
  public void resetLinkParent(){
	  linkToParent=0;
	  linkFromParent=0;
	  for (int p=0;p<subPathwayList.size();p++){
		  subPathwayList.get(p).resetLinkParent();
	  }
  }
   
  public void drawLinkParent(){
	  for (int p=0;p<subPathwayList.size();p++){
		  subPathwayList.get(p).drawLinkParent();
	  }
	  
	  if (parentPathway==null)
		  return;
	 	//  System.out.println("drawLinkParent="+displayName);
		//  System.out.println("linkToParent="+linkToParent);
		//  System.out.println("linkFromParent="+linkFromParent);
		  
		 
	  if (linkToParent>0){
		  parent.stroke(255,0,0);
		  float wei = PApplet.pow(linkToParent, 0.3f);
		  parent.strokeWeight(wei);
		  drawArc(xEntry, yEntry, parentPathway.x, parentPathway.y,wei, true);    // to parent
		 // parent.line(xEntry, yEntry, parentPathway.x, parentPathway.y);
	  }
	  if (linkFromParent>0){
		  parent.stroke(0,255,0);
		  float wei = PApplet.pow(linkFromParent, 0.3f);
		  parent.strokeWeight(wei);
		  drawArc(xEntry, yEntry, parentPathway.x, parentPathway.y,wei,false);
		 // parent.line(xEntry, yEntry, parentPathway.x, parentPathway.y);
	  }
  }
  
  public void drawArc(float x1, float y1, float x2, float y2, float weight,boolean isToParent){
		float xCenter = (x1+x2)/2;
		float yCenter = (y1+y2)/2;
		/*float al1 = PApplet.atan((y1 - yCenter) / (x1 - xCenter));
		float rr = (x1-xCenter)*(x1-xCenter)+(y1-yCenter)*(y1-yCenter);
		rr=PApplet.sqrt(rr);
		parent.noFill();
		System.out.println("rr="+rr+"	al1="+al1);
		 parent.arc(xCenter, yCenter, 2*rr, 2*rr, al1-PApplet.PI, al1);
		*/
		 
		 float dis = (y2-y1)*(y2-y1)+(x2-x1)*(x2-x1);
		 float dd = PApplet.sqrt(dis);
		 float alCircular = PApplet.PI/50;
		
		 float newR = (dd/2)/PApplet.sin(alCircular);
    	 float d3 = PApplet.dist(x1,y1,x2,y2);
    	 float x11 = (x1+x2)/2 - ((y1-y2)/2)*PApplet.sqrt(PApplet.pow(newR*2/d3,2)-1);
    	 float y11 = (y1+y2)/2 + ((x1-x2)/2)*PApplet.sqrt(PApplet.pow(newR*2/d3,2)-1);
    	 float x22 = (x1+x2)/2 + ((y1-y2)/2)*PApplet.sqrt(PApplet.pow(newR*2/d3,2)-1);
    	 float y22 = (y1+y2)/2 - ((x1-x2)/2)*PApplet.sqrt(PApplet.pow(newR*2/d3,2)-1);
    	 
    	 float x3 =0, y3=0;
    	 float d11 = PApplet.dist(x11, y11, xCenter, yCenter);
    	 float d22 = PApplet.dist(x22, y22, xCenter, yCenter);
    	 if (d11>d22){
    		
    	 }
    	 else if (d11<d22){
    		
    	 }
    	 x3=x22;
		 y3=y22;
    	 
		float delX1 = (x1-x3);
		float delY1 = (y1-y3);
		float delX2 = (x2-x3);
		float delY2 = (y2-y3);
		float al1 = PApplet.atan2(delY1,delX1);
		float al2 = PApplet.atan2(delY2,delX2);
		parent.noFill();
		
		// Adding weight
		newR+=weight/2;
		if (isToParent){
			if (al1<al2){
				 x3=x22;
	    		 y3=y22;
				parent.arc(x3, y3, newR*2, newR*2, al1, al2);
			}	
			else{
				 x3=x22;
	    		 y3=y22;
				parent.arc(x3, y3, newR*2, newR*2, al1, al2+2*PApplet.PI);
			}	
		}
		else{
			if (al1<al2){
				 x3=x11;
	    		 y3=y11;
				parent.arc(x3, y3, newR*2, newR*2, al1-PApplet.PI, al2-PApplet.PI);
			}	
			else{
				 x3=x11;
	    		 y3=y11;
				parent.arc(x3, y3, newR*2, newR*2, al1-PApplet.PI, al2+PApplet.PI);
			}	
		}
  }	  
  
  Color getGradient(float value){
   return Color.RED;
  }
}