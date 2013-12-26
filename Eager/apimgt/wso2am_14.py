import logging
from suds.client import Client
from suds.transport.http import HttpAuthenticated
from apimgt.adaptor import APIManagerAdaptor, APIInfo

class WSO2APIManager14Adaptor(APIManagerAdaptor):
  def __init__(self, conf):
    self.url = 'https://{0}:{1}/services/EagerAdmin'.format(conf['host'],
      conf['port'])
    self.__init_service_client(conf)

  def __init_service_client(self, conf):
    if conf.get('debug'):
      logging.basicConfig(level=logging.INFO)
      logging.getLogger('suds.client').setLevel(logging.DEBUG)
    transport = HttpAuthenticated(username=conf['user'], password=conf['password'])
    self.client = Client(self.url + '?wsdl', location=self.url,
      transport=transport, cache=None)

  def is_api_available(self, name, version):
    api = { 'name' : name, 'version' : version }
    return self.client.service.isAPIAvailable(api=api)

  def get_api_list_with_context(self, context):
    results = self.client.service.getAPIsWithContext(context)
    api_list = []
    for result in results:
      api_list.append(APIInfo(result['name'], result['version']))
    return api_list

  def create_api(self, name, version, specification):
    api = { 'name' : name, 'version' : version }
    return self.client.service.createAPI(api=api, specification=specification)