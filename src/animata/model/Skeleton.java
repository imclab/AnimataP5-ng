package animata.model;

import animata.AnimataPlayback;
import animata.model.Mesh.Vertex;
import processing.core.PApplet;
import processing.xml.XMLElement;

public class Skeleton {

	public class AttachedVertex {

		private static final float BONE_MINIMAL_WEIGHT = 0.1f;
		private Vertex vertex;
		private float d;
		private float w;
		private float ca;
		private float sa;
		public float weight;
		private final Bone bone;
		private float dst;

		public AttachedVertex(XMLElement element, Bone bone) {
			this.bone = bone;
			assignAttributes(element);
			setInitialConditions();
		}

		private void setInitialConditions() {
			float x0 = bone.j0.x;
			float y0 = bone.j0.y;
			float x1 = bone.j1.x;
			float y1 = bone.j1.y;

			float alpha = PApplet.atan2(y1 - y0, x1 - x0);
			float dx = (x1 - x0);
			float dy = (y1 - y0);
//			float dCurrent = PApplet.sqrt(dx*dx + dy*dy);
			float x = x0 + dx * 0.5f;
			float y = y0 + dy * 0.5f;

			float vx = vertex.x;
			float vy = vertex.y;
			float vd = PApplet.sqrt((x - vx) * (x - vx) + (y - vy) * (y - vy));

			dst = vd;

			float vdnorm = vd / (bone.attachRadiusMult * bone.dOrig * .5f);

			if (vdnorm >= 1)
			{
				weight = BONE_MINIMAL_WEIGHT;
			}
			else
			{
				weight = PApplet.pow(1.0f - vdnorm, 1.0f / bone.falloff);
			}

			float a = PApplet.atan2(vy - y, vx - x) - alpha;
			sa = vd * (PApplet.sin(a));
			ca = vd * (PApplet.cos(a));
		}

		private void assignAttributes(XMLElement element) {
			vertex = mesh.vertices[element.getIntAttribute("id")];
			d = element.getFloatAttribute("d");
			w = element.getFloatAttribute("w");
			ca = element.getFloatAttribute("ca");
			sa = element.getFloatAttribute("sa");
		}

	}

	public class Bone {

		private static final float BONE_DEFAULT_DAMP = 0.5f;
		private Joint j0;
		private Joint j1;
		private float stiffness;
		private float scale;
		private float maxScale;
		private float minScale;
		private float tempo;
		private float radius;
		private boolean selected;
		private float size;
		private float time;
		private AttachedVertex[] attachedVertices;
		private String name;
		private float damp;
		public float dOrig;
		private float attachRadiusMult;
		private float falloff;

		public Bone(XMLElement element) {
			assignAttributes(element);
			setInitialConditions();
			if(element.getChild("attached") != null) {
				addVertices(element.getChildren("attached/vertex"));
			}
		}

		private void setInitialConditions() {
			damp = BONE_DEFAULT_DAMP;

			float x0 = j0.x;
			float y0 = j0.y;
			float x1 = j1.x;
			float y1 = j1.y;

			dOrig = PApplet.sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));


			attachRadiusMult = 1.0f;
			falloff = 1.0f;

		}

		private void assignAttributes(XMLElement element) {
			name = element.getStringAttribute("name", "");
			j0 = joints[element.getIntAttribute("j0")];
			j1 = joints[element.getIntAttribute("j1")];
			stiffness = element.getFloatAttribute("stiffness");
			scale = element.getFloatAttribute("lm");
			maxScale = element.getFloatAttribute("lmmax");
			minScale = element.getFloatAttribute("lmmin");
			tempo = element.getFloatAttribute("tempo");
			time = element.getFloatAttribute("time");
			size = element.getFloatAttribute("size");
			selected = element.getIntAttribute("selected") == 1;
			radius = element.getFloatAttribute("radius");
		}

		private void addVertices(XMLElement[] children) {
			attachedVertices = new AttachedVertex[children.length];
			for (int i = 0; i < children.length; i++) {
				XMLElement element = children[i];
				AttachedVertex attachedVertex = new AttachedVertex(element,this);
				attachedVertices[i] = attachedVertex;
			}

		}

		public void simulate() {
			if (tempo > 0)
			{
				time += tempo / AnimataPlayback.timeDivision;	// FIXME
				animateScale(0.5f + PApplet.sin(time) * 0.5f);
			}

			float x0 = j0.x;
			float y0 = j0.y;
			float x1 = j1.x;
			float y1 = j1.y;

			float dx = (x1 - x0);
			float dy = (y1 - y0);
			float dCurrent = PApplet.sqrt(dx*dx + dy*dy);

//			if (dCurrent > FLT_EPSILON)
//			{
				dx /= dCurrent;
				dy /= dCurrent;
//			}

			float m = ((dOrig * scale) - dCurrent) * damp;

			if (!j0.fixed )
			{
				j0.x -= m*dx;
				j0.y -= m*dy;
			}

			if (!j1.fixed )
			{
				j1.x += m*dx;
				j1.y += m*dy;
			}

		}

		private void animateScale(float t) {
			scale = minScale + (maxScale- minScale) * t;
		}

		public void translateVertices() {
			float x0 = j0.x;
			float y0 = j0.y;
			float x1 = j1.x;
			float y1 = j1.y;

			float dx = (x1 - x0);
			float dy = (y1 - y0);

			float x = x0 + dx * 0.5f;
			float y = y0 + dy * 0.5f;

			float dCurrent = PApplet.sqrt(dx*dx + dy*dy);
//			if (dCurrent < FLT_EPSILON)
//			{
//				dCurrent = FLT_EPSILON;
//			}
			dx /= dCurrent;
			dy /= dCurrent;

			if(attachedVertices == null) return;
			for (AttachedVertex v : attachedVertices) {
				float vx = v.vertex.x;
				float vy = v.vertex.y;
				float tx = x + (dx * v.ca - dy * v.sa);
				float ty = y + (dx * v.sa + dy * v.ca);
				vx += (tx - vx) * v.weight;
				vy += (ty - vy) * v.weight;
				v.vertex.x = vx;
				v.vertex.y = vy;

			}
		}

	}

	public class Joint {

		private float x;
		private float y;
		private boolean fixed;
		private boolean selected;
		private String name;

		public Joint(XMLElement element) {
			name = element.getStringAttribute("name","");
			x = element.getFloatAttribute("x");
			y = element.getFloatAttribute("y");
			fixed = element.getIntAttribute("fixed") == 1;
			selected = element.getIntAttribute("selected") == 1;
		}

		public void simulate() {
			if(!fixed){
				y += AnimataPlayback.gravity;
			}
		}

	}

	private Joint[] joints;
	private Bone[] bones;
	private final Mesh mesh;

	public Skeleton(XMLElement child, Mesh mesh) {
		this.mesh = mesh;
		addJoints(child.getChildren("joints/joint"));
		addBones(child.getChildren("bones/bone"));
	}

	private void addBones(XMLElement[] children) {
		bones = new Bone[children.length];
		for (int i = 0; i < children.length; i++) {
			XMLElement element = children[i];
			Bone bone = new Bone(element);
			bones[i] = bone;
		}
	}

	private void addJoints(XMLElement[] children) {
		joints = new Joint[children.length];
		for (int i = 0; i < children.length; i++) {
			XMLElement element = children[i];
			Joint joint =  new Joint(element);
			joints[i] = joint;
		}

	}

	public void simulate(int times) {
		for (int i = 0; i < times; i++) {
			for (Joint joint : joints) {
				joint.simulate();
			}
			for(Bone bone : bones){
				bone.simulate();
				bone.translateVertices();
			}
		}

	}

}