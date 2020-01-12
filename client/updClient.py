import os
import sys
import socket
sys.path.append(os.path.abspath(".."))
from udpTools import updClient

FILE = 'resources/clientdata.txt'


def udpClientTest():
    """Udp client function"""
    client = updClient(FILE)
    client.printFileInfo()
    # Create a UDP socket at client side
    client.createUpdSocket()
    client.sendFile()
    print("done sending data")
    client.close()


if __name__== "__main__":
  udpClientTest()
