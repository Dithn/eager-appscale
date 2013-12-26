from time import sleep
import thread
from flexmock import flexmock
from eager import Eager
from eager_service import EagerService
from utils import utils
import SOAPpy
try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase


__author__ = 'hiranya'
__email__ = 'hiranya@appscale.com'

class TestEagerService(TestCase):
  DEFAULT_TEST_PORT = 18881

  def setUp(self):
    (flexmock(utils)
     .should_receive('get_secret')
     .and_return('secret'))
    (flexmock(utils)
     .should_receive('get_adaptor')
     .and_return(None))
    self.service, self.port = self.__start_service()
    sleep(2)


  def test_service(self):
    proxy = SOAPpy.SOAPProxy('http://{0}:{1}'.format('127.0.0.1', self.port))
    result = proxy.ping('wrong_secret')
    self.assertFalse(result['success'])
    self.assertEquals(result['reason'], Eager.REASON_BAD_SECRET)

    result = proxy.ping('secret')
    self.assertTrue(result['success'])
    self.assertEquals(result['reason'], Eager.REASON_ALIVE)

  def __start_service(self):
    port = self.DEFAULT_TEST_PORT
    while True:
      try:
        service = EagerService(port=port, ssl=False)
        thread.start_new_thread(service.start, ())
        return service, port
      except Exception:
        port += 1
        if port > 65535:
          raise IOError('Unable to acquire a port for the test server')

  def tearDown(self):
    flexmock(utils).should_receive('get_secret').reset()
    flexmock(utils).should_receive('get_adaptor').reset()
    self.service.stop()