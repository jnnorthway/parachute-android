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
    server.printFileInfo()


if __name__== "__main__":
  udpServerTest()
