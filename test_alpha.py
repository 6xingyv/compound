with open('sticker.webm', 'rb') as f:
    data = f.read()
    print('AlphaMode (53C0):', b'\x53\xc0' in data)
    print('BlockAdditions (75A1):', b'\x75\xa1' in data)
