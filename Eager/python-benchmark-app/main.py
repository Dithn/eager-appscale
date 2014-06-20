#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

try:
    import json
except ImportError:
    import simplejson as json

from google.appengine.ext import webapp, db
import webapp2
import wsgiref
import math
import random
import time

class Project(db.Model):
    project_id = db.StringProperty(required=True)
    name = db.StringProperty(required=True)
    rating = db.IntegerProperty(required=True)

class ProjectHandler(webapp2.RequestHandler):
    def get(self):
        op = self.request.get('op')
        count = int(self.request.get('count'))
        results = []
        if op == 'put':
            for i in range(count):
                project_id = str('Project' + str(i))
                project_name = 'Project' + str(i)
                project = Project(project_id=project_id,
                                  name=project_name,
                                  rating=int(random.randint(1, 6)),
                                  key_name=project_name)
                t1 = time.clock()
                project.put()
                t2 = time.clock()
                results.append((t2 - t1) * 1000)
        elif op == 'delete':
            for i in range(count):
                project = Project.get_by_key_name('Project' + str(i))
                t1 = time.clock()
                Project.delete(project)
                t2 = time.clock()
                results.append((t2 - t1) * 1000)

        print 'GET /'
        self.response.write(json.dumps(serialize(results)))

    def delete(self):
        db.delete(Project.all())

def serialize(results):
    sum = 0
    for val in results:
        sum += val
    mean = float(sum) / len(results)
    stdDev = 0.0
    if len(results) > 1:
        squareSum = 0;
        for val in results:
            squareSum += (val - mean) * (val - mean)
        stdDev = math.sqrt(squareSum / (len(results) - 1));
    return { 'mean' : mean, 'stdDev' : stdDev }

app = webapp.WSGIApplication([
    ('/python/datastore/project', ProjectHandler)
], debug=True)

if __name__ == '__main__':
    wsgiref.handlers.CGIHandler().run(app)
