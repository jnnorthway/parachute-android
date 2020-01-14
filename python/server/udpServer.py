import os
import sys
import socket
sys.path.append(os.path.abspath(".."))
from udpTools import updServer


def udpServerTest():
    """Udp server function"""
    server = updServer()
    # Create a UDP socket at client side
    server.createUpdSocket()
    server.receiveFile()
    server.close()


def udpServerSimpleTest():
    """Udp server function"""
    server = updServer()
    # Create a UDP socket at client side
    server.createUpdSocket()
    server.UDPSocket.bind(server.server_data['address'])
    while(True):
      data = server.recieveData()[0]
      if data == server.EOF_MSG:
        break
      print('received: %s' % server.decode(data))
    server.close()


if __name__== "__main__":
  # udpServerTest()
  udpServerSimpleTest()