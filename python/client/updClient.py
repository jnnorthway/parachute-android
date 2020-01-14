import os
import sys
import socket
sys.path.append(os.path.abspath(".."))
from udpTools import updClient

FILE = 'resources/clientdata.txt'
# FILE = '/mnt/sf_VM_Coding/textbook3.pdf'
# FILE = '/home/jnorthway/Downloads/bird.jpg'


def udpClientTest():
    """Udp client function"""
    client = updClient(FILE)
    client.printFileInfo()
    # Create a UDP socket at client side
    client.createUpdSocket()
    client.sendFile()
    client.close()


def udpClientSimpleTest():
    """Udp client function"""
    client = updClient(FILE)
    # Create a UDP socket at client side
    client.createUpdSocket()
    print('sending Hello')
    client.sendRawData(b'Hello')
    client.close()


if __name__== "__main__":
  # udpClientTest()
  udpClientSimpleTest()
