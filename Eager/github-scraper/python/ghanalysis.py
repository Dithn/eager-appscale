from time import sleep
import datetime
import getpass
import keyring
import traceback
from github import Github

DEBUG = False
earliest = datetime.datetime(2012,1,1)
 
def getAERepos(username):
   count = 0
   try:
	g = Github(username, getGithubPassword(username))
	for repo in g.legacy_search_repos('app engine'):
	   count += 1
	   try:
	      #if repo.updated_at > earliest or repo.pushed_at > earliest:
	      if repo.pushed_at > earliest:
		try:
		   print '{0};{1};{2};{3};{4};{5};{6};{7};{8}'.format(
		      repo.name,
		      repo.created_at.date(),
		      repo.updated_at.date(),
		      repo.pushed_at.date(),
		      repo.owner.login,
		      repo.language,
		      repo.forks,
		      repo.watchers,
		      repo.description)
		except:
			print 'ERROR unable to print description of repo {0}'.format(repo.name)
	     	if DEBUG and count > 10: 
	           break
	      if 'appscale' in repo.name.lower():
	         print '\tFound AppScale!' 
	   except:
	      print 'ERROR1 unable to get repo'
	   sleep(2)
   except:
	print 'ERROR2 unable to get anything'
		
 
def printRepository(username):
	g = Github(username, getGithubPassword(username))
 
	user = g.get_user()
	repositories = user.get_repos()
 
	for repository in repositories:
		print repository.name
		printBranches(repository)
 
def printBranches(repository):
	for branch in repository.get_branches():
		print '  ', branch.name
		tree = branch.commit.commit.tree
		printTree(repository, tree, '    ')
 
def printTree(repository, tree, indent):
	for element in tree.tree:
		print indent, element.path
		if element.type == 'tree':
			printTree(repository, repository.get_git_tree(element.sha), indent + '  ')
 
def getGithubPassword(username):
	service = 'github'
	password = keyring.get_password(service, username)
	if password == None:
		print "Enter password for user", username
		password = getpass.getpass()
		keyring.set_password(service, username, password)
	return password
 
# Pass your Github username as a parameter
#printRepository('ckrintz')

# step through the repos with keyword 'app engine'
getAERepos('ckrintz')
