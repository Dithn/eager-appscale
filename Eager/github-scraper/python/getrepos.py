import requests
import json
from json import dumps
#r = requests.get('https://api.github.com/repos/django/django')
r = requests.get('https://api.github.com/legacy/repos/search/:app engine')
if(r.ok):
   repoItem = json.loads(r.text or r.content)
   print "created_at: " + repoItem['created_at']
   print "pushed: " + repoItem['pushed_at']
   print "private: " + str(repoItem['private'])
   print "id: " + str(repoItem['id'])
   print "owner: " + repoItem['owner']['login']
   print "description: " + repoItem['description']
   print "forks: " + str(repoItem['forks'])
   print "watchers: " + str(repoItem['watchers'])
   print "has downloads: " + str(repoItem['has_downloads'])


#   print dumps(repoItem, indent=4)
