# Compound

**Compound** is a streamlined Android messaging client built on top of **TDLib**.

## ⚖️ License

This project is licensed under **CC BY-ND 4.0**.

* **Allowed:** Cloning and forking for personal use or submitting **Pull Requests**. We love contributions!
* **Prohibited:** Distributing modified versions (forks) or re-packaged APKs.

> **Note to Contributors:** To prevent ecosystem fragmentation and ensure security, all improvements must be submitted via PR to the official repository. Publicly hosting a modified fork is not permitted.


## 📦 Build

1. **Clone the repository**:
    ```bash
    git clone https://github.com/6xingyv/compound.git
    ```

2. **Setup configs**:

   Before building the project, you must configure your environment. Create a local.properties file in the root directory and add the following:
   ```properties
   # Telegram API ID and Hash
   API_ID=YOUR_API_ID
   API_HASH=YOUR_API_HASH
   # Signing Config
   RELEASE_STORE_FILE=/Users/yourname/keys/my-release-key.jks
   RELEASE_STORE_PASSWORD=your_password
   RELEASE_KEY_ALIAS=your_alias
   RELEASE_KEY_PASSWORD=your_password
   ```
   Where the **Telegram API ID and Hash** can be obtained from https://my.telegram.org.

3. **Build**

## 🤝 Contributing

We welcome contributions that align with the project's vision!

1. Open an issue to discuss the change.
2. Submit a Pull Request.
3. Once merged, your changes will be part of the official distribution.

To prevent ecosystem fragmentation and ensure security. All improvements should be submitted via Pull Requests to the main repository.

---

*Disclaimer: This project is not officially affiliated with Telegram Messenger LLP.*
