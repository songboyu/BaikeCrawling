#coding=utf-8
import re
import os
import dircache
import datetime

output_dir = 'baike_data_final/'

entriesFile 	= open(output_dir + 'entries.txt','w')
tagsFile 		= open(output_dir + 'tags.txt','w')
relationFile 	= open(output_dir + 'relations.txt','w')
inlinksFile 	= open(output_dir + 'inlinks.txt','w')

tagMap = {}
entSet = set()

''''打印时间间隔'''
def printTimeDiff(old):
	now = datetime.datetime.now()
	timeDiff = (now - old).seconds
	hour = timeDiff / (60*60)
	minute = timeDiff / 60 - hour*60
	seconds = timeDiff - hour*60*60 - minute*60
	print'Cost %s h %s min %s s' % (hour, minute, seconds)

'''写入词汇实体'''
def insertEntries(entId, title, summary):
	if entId in entSet:
		return None
	entSet.add(entId)
	entriesFile.write(entId+'\t'+title+'\t'+summary+'\n')
	return entId

'''写入类别'''
def insertTags(tags):
	if len(tags) == 0:
		return
	tagIds = []
	for tag in tags:
		tag = re.sub(r'\\','',tag)
		tag = re.sub(r'\s','',tag)
		if tag.strip() == '':
			continue
		tid = 0
		if tag in tagMap:
			tid = tagMap[tag]
		else:
			tid = len(tagMap)+1
			tagMap[tag] = tid
			tagsFile.write(str(tid)+'\t'+tag+'\n')
		if tid not in tagIds:
			tagIds.append(tid)
	return tagIds

'''写入实体与类别关系'''
def insertRelations(entId, tagIds):
	if len(tagIds) == 0:
		return
	for tid in tagIds:
		relationFile.write(str(entId)+'\t'+str(tid)+'\n')

'''写入inlinks'''
def insertInlinks(entId, inlinks):
	if len(inlinks) == 0:
		return
	for inlink in inlinks:
		if inlink.strip() == '':
			continue
		inlinksFile.write(str(entId)+'\t'+inlink+'\n')

'''处理主函数'''
def startProcData():
	count = 0
	fileDir = 'baike_data/'
	pattern = re.compile(r'^\[(.*?)\]\{(.*?)\}<(.*?)>\((.*)\)\【(.*?)\】$')
	old = datetime.datetime.now()

	# 遍历文件夹下所有文件
	fileList = dircache.listdir(fileDir)
	for fileName in fileList:
		filePath = fileDir + fileName
		if (not os.path.exists(filePath)):
			continue
		for line in open(filePath, 'r'):
			m = pattern.match(line)
			if m:
				# ID
				entId = m.group(1)
				# 标题
				title = m.group(2)
				# 类别
				tags = m.group(3).split(',')
				# 摘要
				summary = m.group(4)
				summary = summary[0:498] if len(summary) > 499 else summary
				# inlinks
				inlinks = set(m.group(5).split(','))
				
				# 写入entries
				if insertEntries(entId, title, summary) != None:
					# 写入inlinks
					insertInlinks(entId, inlinks)
				# 写入tags
				tagIds = insertTags(tags)
				if tagIds!=None and len(tagIds) > 0:
					# 写入relations
					insertRelations(entId, tagIds)
					

			count += 1
			if count % 100000 == 0:
				print 'There is ' + str(count) + ' records inserted.'
				printTimeDiff(old)

	print 'There is ' + str(count) + ' records inserted.'
	printTimeDiff(old)

if __name__ == '__main__':
	startProcData()

	entriesFile.close()
	tagsFile.close()
	relationFile.close()
	inlinksFile.close()