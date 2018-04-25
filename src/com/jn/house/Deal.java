package com.jn.house;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
@SuppressWarnings("unused")
public class Deal {
	public static String[] mailList=new String[]{};
	private static String lastNum="0";//上一次成交次数
	private static String nowNum="0";//本次成交次数
	public static void main(String args[])throws Exception{  
		scheduleTask();//启动定时任务
    }  
	/***
	 * 解析网页数据
	 * @return
	 */
	public static String parseSaleToday(){
		try {
			String jndfc_webSite=getProperty("jndfc_webSite");
			InetAddress netAddress = InetAddress.getLocalHost();   
			StringBuffer sb_area=new StringBuffer();
			sb_area.append("<table style='border-collapse: collapse;' bordercolor='#3c8ade' border='1px' align='center'  width='60%'><tr  height='45px' bgcolor='#c7e5ff'><td colspan='5' align='center'><h2>全市成交汇总</h2></td></tr><tr height='45px' bgcolor='#c7e5ff' style='font-size:14px;font-weight:bold;'><td>地区</td><td>可售套数</td><td>住宅可售套数</td><td>今日签约套数</td><td>今日签约面积 </td></tr>");
			 //获取每日成交
			Document doc = Jsoup.connect(jndfc_webSite+"/saletoday/index.shtml").timeout(10000000).get();
			Elements project_table = doc.getElementsByClass("project_table");  
			//loupan_doc共计3个，第一个表头，第二个开发商成交 第三个区域成交记录
			if(project_table.size()>2){
				Element Area=project_table.get(2);
				Elements tr=Area.getElementsByTag("tr");
				//处理全市总览
				for (Element trs : tr) {  
					Elements td=trs.getElementsByTag("td");
					if(td.size()!=0){
						if(getProperty("area").equals(td.get(1).text())){
							sb_area.append("<tr  height='30px' style='color:red'><td>"+td.get(1).text()+"</td><td>"+td.get(2).text()+"</td><td>"+td.get(3).text()+"</td><td>"+td.get(4).text()+"</td><td>"+td.get(5).text()+"</td></tr>");
							lastNum=nowNum;
							nowNum=td.get(4).text();
						}else{
							sb_area.append("<tr height='30px'><td>"+td.get(1).text()+"</td><td>"+td.get(2).text()+"</td><td>"+td.get(3).text()+"</td><td>"+td.get(4).text()+"</td><td>"+td.get(5).text()+"</td></tr>");
						}
					}
				}
				sb_area.append("</table>");
			}
			//处理我关注的楼盘
			int i=1;
			boolean flag=false;
			StringBuffer judgeFlag=new StringBuffer();
			String[] attentionKeyWords=getProperty("attentionKeyWord").split(",");//关注楼盘汉字
			StringBuffer sb_loupan=new StringBuffer();
			sb_loupan.append("<br><br><table style='border-collapse: collapse;' bordercolor='#3c8ade' border='1px'  align='center' width='60%'><tr height='45px'><td colspan='4' align='center' bgcolor='#c7e5ff'><h2>我关注的楼盘今日成交情况</h2></td></tr><tr bgcolor='#c7e5ff' height='30px' style='font-size:14px;font-weight:bold;'><td>成交序号</td><td>楼盘详情</td><td>成交量</td><td>签约面积</td></tr>");
			Element loupan=project_table.get(1);
			Elements tr_loupan=loupan.getElementsByTag("tr");
			//处理我关注的楼盘
			for (Element trs : tr_loupan) {  
				Elements td=trs.getElementsByTag("td");
				if(td.size()!=0){
					String td_text=td.get(1).text();
					for(int a=0;a<attentionKeyWords.length;a++){
						if(td_text.contains(attentionKeyWords[a])){
							i=0;
							sb_loupan.append("<tr height='30px'><td>"+td.get(0).text()+"</td><td>"+td.get(2).text()+"--"+td.get(3).text()+"</td><td>"+td.get(4).text()+"</td><td>"+td.get(5).text()+"</td><tr>");
						}
					}
				}
			}
			if(i==1){
				sb_loupan.append("<tr height='30px'><td colspan='4'>暂无成交记录</td></tr>");				
			}
			sb_loupan.append("</table>");
			//处理楼盘详情
			//获取楼盘详情
			String[] prjno=getProperty("prjno").split(",");//关注楼盘查询id
			StringBuffer detailStr=new StringBuffer();
			for(int a=0;a<prjno.length;a++){
				detailStr.append("<br><br><table style='border-collapse: collapse;' bordercolor='#3c8ade' border='1px'  align='center' width='60%'><tr height='45px' bgcolor='#c7e5ff'><td colspan='9' align='center'><h2>"+attentionKeyWords[a]+"-楼盘详情</h2></td></tr>");
				detailStr.append("<tr height='30px' bgcolor='#c7e5ff'><td>序号</td><td>楼盘名称</td><td>预售许可证</td><td>总套数</td><td>总面积</td><td>可售套数</td><td>可售面积</td><td>已售套数</td><td>已售面积</td></tr>");
				Document loupan_doc = Jsoup.connect(jndfc_webSite+"/onsaling/show.shtml?prjno="+prjno[a]).timeout(10000000).get();  
				Elements details=loupan_doc.getElementsByClass("project_table");
				Element loupan_details=details.get(0);
				Elements loupan_details_tr=loupan_details.getElementsByTag("tr");
				//处理我关注的楼盘
				for(int j=0;j<loupan_details_tr.size()-1;j++){
					Elements td=loupan_details_tr.get(j).getElementsByTag("td");
					if(td.size()!=0&&!td.contains("span")){
						detailStr.append("<tr  height='30px'><td>"+td.get(0).text()+"</td><td>"+td.get(1).attr("title")+"</td><td>"+td.get(2).attr("title")+"</td><td>"+td.get(3).text()+"</td><td>"+td.get(4).text()+"</td><td>"+td.get(5).text()+"</td><td>"+td.get(6).text()+"</td><td>"+td.get(7).text()+"</td><td>"+td.get(8).text()+"</td><tr>");
					}
				}
				detailStr.append("</table>");
			}
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)+2);
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sb_area+sb_loupan.toString()+"<br>"+detailStr+"<hr>from："+netAddress+" 下次获取时间："+df.format(calendar.getTime());
		} catch (Exception e) {
			e.printStackTrace();
			return "连接失败";
		}
	}
	public synchronized static String ExplorUrlDate() {
		Timestamp ts = new Timestamp(new Date().getTime());
		String tsStr = "";   
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   
		try {   
			tsStr = sdf.format(ts);   
		} catch (Exception e) {   
		    e.printStackTrace();   
		}  
		return tsStr;
	}
	//发送邮件
	public static boolean sendMail(String date,String content){
		 try {
		     Properties prop = new Properties();
			 prop.setProperty("mail.host",getProperty("mailSendSMTP"));
			 prop.setProperty("mail.transport.protocol", "smtp");
			 prop.setProperty("mail.smtp.auth", "true");
			 //使用JavaMail发送邮件的5个步骤
			 //1、创建session
			 Session session = Session.getInstance(prop);
			 //开启Session的debug模式，这样就可以查看到程序发送Email的运行状态
			 session.setDebug(true);
			 //2、通过session得到transport对象
			 Transport ts = session.getTransport();
			 InternetAddress from = new InternetAddress(getProperty("mailFrom")); 
			 //3、使用邮箱的用户名和密码连上邮件服务器，发送邮件时，发件人需要提交邮箱的用户名和密码给smtp服务器，用户名和密码都通过验证之后才能够正常发送邮件给收件人。
			 ts.connect(getProperty("mailSendSMTP"), getProperty("mailSendUserName"),getProperty("mailSendPassword"));
			 //创建邮件对象
			  MimeMessage message = new MimeMessage(session);
			  String nick=javax.mail.internet.MimeUtility.encodeText(getProperty("mailTitle"));
			  //指明邮件的发件人
			  message.setFrom(new InternetAddress(nick+" <"+from+">"));
			  mailList=getProperty("mailList").split(",");
			  Address[] addr=new InternetAddress[mailList.length];
			  for(int i=0;i<mailList.length;i++){
				  if(!StringUtil.isBlank(mailList[i])){
					  addr[i] = new InternetAddress(mailList[i]);
				  }
			  }
			  message.setRecipients(Message.RecipientType.TO,addr);
			  //邮件的标题
			  message.setSubject("【成交记录】"+date);
			  //邮件的文本内容
			  message.setContent(content, "text/html;charset=UTF-8");
			  //返回创建好的邮件对象
			 ts.sendMessage(message, message.getAllRecipients());
			 ts.close();
			 return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
	}
	  public static void scheduleTask() {
	        Timer timer = new Timer();
	        timer.scheduleAtFixedRate(new TimerTask() {
	            public void run() {
	            	Date date=new Date();
	            	//处理跨过周末和凌晨情况
	            	 Calendar rightNow=Calendar.getInstance();  
	                 int day=rightNow.get(rightNow.DAY_OF_WEEK);//获取时间 
	            	if(date.getHours()>=7&&date.getHours()<=20){
	            		System.out.println(ExplorUrlDate()+"访问今日成交网站开始");
						String now= parseSaleToday();
						System.out.println(ExplorUrlDate()+"解析今日成交网站完毕");
						//String prettyStr="本次变化数:"+(Integer.parseInt(lastNum)==0?"【首次获取数据，暂无变化】":Integer.parseInt(nowNum)-Integer.parseInt(lastNum))+"    本次获取结果："+now;
						String prettyStr=now;
						System.out.println(prettyStr);
						if(!"连接失败".equals(now)){
							if(sendMail(ExplorUrlDate(),prettyStr)){
								 System.out.println("邮件发送成功");
							}else{
								 System.out.println("邮件投递失败");
							}
						}
	            	}
	            }
	        }, 0, 2000*60*60);// 每个小时轮询一次
	    }
	  /***
	   * 获取配置文件key value内容
	   * @param name
	   * @return
	   */
	  public static String getProperty(String name){
		    InputStream is =Deal.class.getClassLoader().getResourceAsStream("project.properties");
			Properties p = new Properties();
			try {
				p.load(new InputStreamReader(is, "UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return p.getProperty(name);
	  }
}
