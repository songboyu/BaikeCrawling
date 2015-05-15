import re
import requests

f = open('result.txt','w')
for i in range(6200000, 6300000):
	r = requests.get('http://baike.baidu.com/view/'+str(i)+'.htm')
	if re.findall(r'/subview/(\d+/\d+).htm|/view/(\d+).htm', r.url):
		print r.url
		f.write(r.url+'\n')