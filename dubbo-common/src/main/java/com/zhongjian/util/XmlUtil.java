package com.zhongjian.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlUtil {
	// 1.doc解析
	/*
	 * 解析思路: 1.使用Doc类进行解析,首先创建工厂对象 2.使用工厂对象创建DocBuider 3.使用DocumentBuilder的方法
	 * parse(xml路径) 可以获得完整的XML文件内容 4.完成的XML文件内容使用Document进行接收 5.使用Document中的
	 * getDocumentElement();可以获得XML的根节点 6.根节点的方法getElementsBytagName(子节点)
	 * 传入子节点可以获取子节点的所有内容 7.所有子节点使用NodeList进行接收存储.只需要进行遍历即可得出每个子节点的内容.
	 * 8.每一个节点是一个NODE类型.可以使用item(i)获取每一个节点
	 * 9.子节点中也有子节点,可以使用getChildNodes()获取子节点中所有节点,还是NodeList保存 10.使用节点中的
	 * getNodeName()可以获取子节点的名称 11.通过判断子节点的名称来设置对应的值. 获取值使用
	 * getFirseChild().getNodeValue();即可.
	 */
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
		return factory.newDocumentBuilder(); // 获得一个doc实例
	}

	public static Map<String, String> flGetOrderNo(InputStream is) throws Exception {
	    DocumentBuilder buider = XmlUtil.getDocumentBuilder();
		Document doc = buider.parse(is); // 传入XML路径,返回解析后的Doc类.
		NodeList Items = doc.getElementsByTagName("Message");// 获取了所有BOOK节点
		int itemsLegth = Items.getLength();
		Map<String, String> res = new HashMap<String, String>();
		for (int i = 0; i < itemsLegth; i++) {
			// 获取子节点
			Node nodes = Items.item(i); // 要从子节点中继续获取节点
			NodeList ChildNodes = nodes.getChildNodes(); // 如果有子节点就是用这个
			int length = ChildNodes.getLength();
			for (int j = 0; j < length; j++) {

				Node ChildNode = ChildNodes.item(j);
				String TagName = ChildNode.getNodeName();
				// 判断标签是哪个名字
				if (TagName.equals("MerSeqNo")) {
					String originOrderNo = ChildNode.getFirstChild().getNodeValue();
					res.put("out_trade_no", originOrderNo.substring(6));
				}else if (TagName.equals("TransAmt")) {
					String total = ChildNode.getFirstChild().getNodeValue();
					res.put("total", total);
				}

			}

		}
		return res;
	}

}
