package com.baike;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baike.model.PageContent;
import com.baike.util.WikiHttpClient;

public class Spider {

	private int 		START_INDEX = 1;
	private int 		MAX_INDEX 	= 150000;
	private final int		THREAD_COUNT= 10;
	private final int		PARSE_COUNT	= 10;
	
	private final String 	BASE_URI 	= "http://baike.baidu.com/view/";
	private final String 	SUB_URI 	= "http://baike.baidu.com/subview/";
	private final String 	TITLE_REX 	= "<title>\\s*(.*?)\\s*(（(.*?)）)*?\\s*_.*</title>";
	private final String 	SUMMARY_REX = "<meta name=\"Description\" content=\"(.*?)\"\\s*/>";
	private final String 	LEMMA_REX 	= "<a target=_blank href=\"/subview/(.*?)\">";
	private final String 	ISPARENT_REX= "<div id=\"lemma-list\"";
	private final String 	ID_REX 		= "href=\"/subview/(\\d+/\\d+).htm|href=\"/view/(\\d+).htm";
	private final String 	TAG_ITEM_REX= "<sapn class=\"taglist\">(.*?)</sapn>";
	private final String	TAG_REX		= "<dd id=\"open-tag-item\">(.*?)</dd";
	private final String	TAG_A_REX	= "<a target=.*?>(.*?)</a>";
	
	private	Pattern tagAPattern = Pattern.compile(TAG_A_REX);
	private	Pattern idPattern 	= Pattern.compile(ID_REX);
	private	Pattern paPattern 	= Pattern.compile(ISPARENT_REX);
	private	Pattern titlePattern= Pattern.compile(TITLE_REX);
	private Pattern tagPattern 	= Pattern.compile(TAG_REX);
	private Pattern tagIPattern = Pattern.compile(TAG_ITEM_REX);
	private	Pattern lemmaPattern= Pattern.compile(LEMMA_REX);
	private	Pattern sumPattern 	= Pattern.compile(SUMMARY_REX);
	
	private AtomicInteger 	a			= new AtomicInteger(1);
	private AtomicInteger	b			= new AtomicInteger(0);
	private AtomicInteger	count		= new AtomicInteger(0);
	private AtomicInteger	finishCount	= new AtomicInteger(0);
	private long			startTime 	= 0;
	private boolean		finished	= false;
	private boolean		procFinished= false;
	private ConcurrentLinkedQueue<PageContent> contentQueue = new ConcurrentLinkedQueue<PageContent>();
	private ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<String>();
	
	public Spider(){
		a = new AtomicInteger(1);
	}
	
	public Spider(int max, int start){
		this.MAX_INDEX = max;
		this.START_INDEX = start;
		a = new AtomicInteger(START_INDEX);
	}
	
	public void startCrawling(){
		startTime = System.currentTimeMillis();
		for(int i = 0;i < THREAD_COUNT;i++){
			SpiderThread st 		= new SpiderThread();
			st.start();
		}
		for(int i = 0;i < PARSE_COUNT;i++){
			ProcContentThread pt 	= new ProcContentThread();
			pt.start();
		}
		WriteDataThread wt 		= new WriteDataThread();
		wt.start();
	}
	
	private String getContentByUri(String uri){
		String content = WikiHttpClient.get(uri);
		if(content.equals("403")){
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return getContentByUri(uri);
		}
		return content;
	}
	
	private class SpiderThread extends Thread{
		
		@Override
		public void run() {
			super.run();
			getContent();
		}
		
		public void getContent(){
			while(true){
				int i = a.getAndAdd(1);
				
				if(i > MAX_INDEX){
					finished = true;
					break;
				}
				String content = getContentByUri(BASE_URI + i + ".htm?force=1");
				if(!content.equals("403") && !content.equals("invalid"))
					contentQueue.add(new PageContent(content,i+"", false));
			}
		}
		
	}
	
	private class ProcContentThread extends Thread{
		@Override
		public void run() {
			super.run();
			procContent();
		}
		
		private void procContent(){
			int x = 0;
			while(true){
				while(true){
					PageContent content = null;
					synchronized (contentQueue) {
						if(!contentQueue.isEmpty())
							content = contentQueue.poll();
						else
							break;
					}
					getDataFromContent(content);
					int c = count.addAndGet(1);
					long now = System.currentTimeMillis();
					if((now - startTime) / 60000 > b.get()){
						int d = b.addAndGet(1);
						System.out.println("At " + d + "min conent count:"+c);
					}
					x = 0;
				}
				try {
					Thread.sleep(1000);
					synchronized (contentQueue) {
						if(finished && contentQueue.isEmpty()){
							x++;
							if(x > 60)
							{
								procFinished = true;
								break;
							}
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			finishCount.addAndGet(1);
		}
		
		private void getDataFromContent(PageContent content){
			if(!content.isSub())
			{
				Matcher paMatcher = paPattern.matcher(content.getContent());
				if(paMatcher.find()){
					getSubData(content.getContent());
					return;
				}
			}
			Matcher titleMatcher = titlePattern.matcher(content.getContent());
			String title = "";
//			String type = "::";
			String summary = "";
			String tags = "";
			String ids = "";
			
			if(titleMatcher.find()){
				title = titleMatcher.group(1);
//				type += titleMatcher.group(3);
				Matcher sumMatcher = sumPattern.matcher(content.getContent());
				if(sumMatcher.find())
				{
					summary += sumMatcher.group(1);
				}
				sumMatcher = null;
			}else
			{
				return;
			}
			
			Matcher idMatcher = idPattern.matcher(content.getContent());
			while(idMatcher.find()){
				String id = idMatcher.group(1);
				if(id == null){
					id = idMatcher.group(2);
				}else{
					id = "s_"+id;
				}
				if(id != null && !id.equals("") && !id.equals(content.getId())){
					if(ids.equals("")){
						ids += id;
					}else{
						ids += "," + id;
					}
				}
			}
			
			Matcher tagMatcher = tagPattern.matcher(content.getContent());
			if(tagMatcher.find()){
				String tagL = tagMatcher.group(1);
				Matcher tagIMatcher = tagIPattern.matcher(tagL);
				while(tagIMatcher.find())
				{
					String tag = tagIMatcher.group(1);
					Matcher tagAMatcher = tagAPattern.matcher(tag);
					if(tagAMatcher.find()){
						tag = tagAMatcher.group(1);
					}
					if(tag != null && !tag.equals("")){
						if(tags.equals("")){
							tags += tag;
						}else{
							tags += "," + tag;
						}
					}
				}
			}
			if(!title.equals("")){
				String line = "["+content.getId()+"]{"+title+"}<"+ tags+">("  + summary + ")【" + ids + "】\n";
				dataQueue.add(line);
			}
			tagMatcher = null;
			titleMatcher = null;
		}
		
		private void getSubData(String doc){
			ProcSubContentThread pst 		= new ProcSubContentThread(doc);
			pst.start();
		}
		
	}

	private class ProcSubContentThread extends Thread{

		private String doc = "";

		public ProcSubContentThread(String doc){
			this.doc = doc;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			getSubData(doc);
		}
		
		private void getSubData(String doc){
			Matcher lemmaMatcher = lemmaPattern.matcher(doc);
			while(lemmaMatcher.find()){
				
				String id_uri = lemmaMatcher.group(1);
				String sub_uri = SUB_URI + id_uri;
				String content = getContentByUri(sub_uri);
				String Id = id_uri.substring(0,id_uri.length() - 4);
				if(!content.equals("403") && !content.equals("invalid"))
					contentQueue.add(new PageContent(content, "s_"+Id, true));
			} 
			lemmaMatcher = null;
			this.doc = null;
		}
	}
	
	private class WriteDataThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			long old = System.currentTimeMillis();
			writeData();
			long now = System.currentTimeMillis();
			System.out.println("Cost " 
					+ (now - old) / 3600000 + " h " 
					+ ((now - old) % 3600000) / 60000 + " min " 
					+ ((now - old) % 60000) / 1000 + " s ");
			System.exit(1);
		}
		
		private void writeData(){
			File f = new File("baike_data/original-"+START_INDEX+".txt");
			BufferedWriter bw = null;
			while(bw == null){

				try {
					if(!f.exists()){
						bw = new BufferedWriter(new FileWriter(f));
					}
					else
						bw = new BufferedWriter(new FileWriter(f,true));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			int x = 0;
			while(true){
				while(!dataQueue.isEmpty()){
					x = 0;
					try {
						bw.write(dataQueue.poll());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				try {
					bw.flush();
					Thread.sleep(3000);
					if(procFinished && dataQueue.isEmpty()){
						x++;
						if(x > 20)
							break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
