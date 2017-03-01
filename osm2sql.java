// Parses .osm-file into database-input
//
// Usage: osm2sql filename
// Output on Std-Out.
//
// 2008-09-27 Andreas "goblor" Hahn

import java.io.*;
import javax.xml.parsers.SAXParserFactory;  
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

public class osm2sql 
{
	
	public static void main (String argv[])
	{
	
		if (argv.length != 1) 
		{
			System.err.println ("Usage: osm2sql filename");
			System.exit (1);
		}
		System.out.println("set character set utf8;");
		System.out.println("SET AUTOCOMMIT=0;");
		try 	
		{
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse( new File(argv [0]), new Handler() );
		}
		catch (Throwable t) 
		{
			t.printStackTrace ();
		}
		System.out.println("COMMIT;");
		System.exit (0);
	}
}

class Handler extends DefaultHandler
{
	String nodeid="";
	String wayid="";
	String relationid="";
	boolean intag=false;
	boolean inmember=false;
	
	int sequence=1;
	
	String[] nodeelements = {"id", "lat", "lon", "visible", "user", "timestamp"};
	String[] wayelements = {"id", "visible", "user", "timestamp"};
	String[] relationelements = {"id", "visible", "user", "timestamp"};

	public String converttimestamp(String t)
	{
		return t.replace('T',' ').substring(0, t.length()-1);
	}

	public String arrayjoin(String[] arr, String delimiter)
	{
		String s="";
		for (int i=0; i<arr.length-1; i++)
		{
			s += arr[i]+delimiter;
		}
		return s+arr[arr.length-1];
	}
	
	//creates "INSERT INTO .... VALUES ....
	public String simpleTableInput(String tablename, String[] elements, Attributes attributes)
	{
		String elementstring;
		String s = "INSERT INTO ";
		s += tablename;
		s += " (";
		s += arrayjoin(elements, ", ");
		s += ") VALUES (";
		for (int i=0; i < elements.length-1; i++)
		{
			elementstring = attributes.getValue(elements[i]);
			if (elements[i].equals("timestamp"))
			{
				s += "'"+converttimestamp(elementstring)+"', ";
			}
			else
			{

				if (elementstring != null)
				{
					s += "'"+ elementstring.replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'") + "', ";
				}
				else
				{
					s += "NULL, ";
				}
			}
		}
		elementstring = attributes.getValue(elements[elements.length-1]);
		if (elements[elements.length-1].equals("timestamp"))
		{
			s += "'"+converttimestamp(elementstring)+"');";	
		}
		else
		{
			if (elementstring != null)
			{
				s += "'"+elementstring.replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'")+"');";
			}
			else
			{
				s += "NULL);";
			}
		}
		
		return s;
	}

	//Handles XML-Elements
	public void startElement (String uri, String localName, String qName, Attributes attrs) 
	{
		if (qName.equals("node"))
		{
			nodeid = attrs.getValue("id");
			System.out.println(simpleTableInput("nodes", nodeelements, attrs));
		}
		if (qName.equals("way"))
		{
			wayid =attrs.getValue("id");
			System.out.println(simpleTableInput("ways", wayelements, attrs));
		}
		if (qName.equals("relation"))
		{
			relationid = attrs.getValue("id");
			System.out.println(simpleTableInput("relations", relationelements, attrs));
		}
		if (qName.equals("tag"))
		{
			intag = true;
			if (!nodeid.isEmpty())
			{
				System.out.print("INSERT INTO node_tags (id, k, v) VALUES ('");
				System.out.print(nodeid);
				System.out.print("', '");
				System.out.print(attrs.getValue("k").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.print("', '");
				System.out.print(attrs.getValue("v").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.println("');");
			}
			if (!wayid.isEmpty())
			{
				System.out.print("INSERT INTO way_tags (id, k, v) VALUES ('");
				System.out.print(wayid);
				System.out.print("', '");
				System.out.print(attrs.getValue("k").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.print("', '");
				System.out.print(attrs.getValue("v").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.println("');");			
			}
			if (!relationid.isEmpty())
			{
				System.out.print("INSERT INTO relation_tags (id, k, v) VALUES ('");
				System.out.print(relationid);
				System.out.print("', '");
				System.out.print(attrs.getValue("k").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.print("', '");
				System.out.print(attrs.getValue("v").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.println("');");
			}			
		}
		if (qName=="nd") //way_node
		{
			if (!wayid.isEmpty())
			{
				System.out.print("INSERT INTO ways_nodes (wayid, nodeid, sequence) VALUES ('");
				System.out.print(wayid);
				System.out.print("', '");
				System.out.print(attrs.getValue("ref").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
				System.out.print("', '");
				System.out.print(sequence);
				System.out.println("');");
				sequence++;
			}
		}
		if (qName.equals("member"))
		{
			inmember = true;
			if (!relationid.isEmpty())
			{
				if (attrs.getValue("type").equals("way"))
				{
					System.out.print("INSERT INTO member_way (wayid, relid, role) VALUES ('");
					System.out.print(attrs.getValue("ref"));
					System.out.print("', '");
					System.out.print(relationid);
					System.out.print("', '");
					System.out.print(attrs.getValue("role").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
					System.out.println("');");
				}
				if (attrs.getValue("type").equals("node"))
				{
					System.out.print("INSERT INTO member_node (nodeid, relid, role) VALUES ('");
					System.out.print(attrs.getValue("ref"));
					System.out.print("', '");
					System.out.print(relationid);
					System.out.print("', '");
					System.out.print(attrs.getValue("role").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
					System.out.println("');");
				}				
				if (attrs.getValue("type").equals("relation"))
				{
					System.out.print("INSERT INTO member_relation (relid2, relid, role) VALUES ('");
					System.out.print(attrs.getValue("ref"));
					System.out.print("', '");
					System.out.print(relationid);
					System.out.print("', '");
					System.out.print(attrs.getValue("role").replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'"));
					System.out.println("');");
				}				
			}
		}
	}

	public void endElement (String uri, String localName, String qName)  
	{
		if (qName.equals("node"))
		{
			nodeid = "";
		}
		if (qName.equals("way"))
		{
			wayid = "";
			sequence = 1;
		}
		if (qName.equals("relation"))
		{
			relationid = "";
		}
		if (qName.equals("tag"))
		{
			intag = false;
		}
		if (qName.equals("member"))
		{
			inmember = false;
		}
	}


	public void startDocument() {}
	public void endDocument () {}
	public void characters (char buf [], int offset, int len) {}
}

