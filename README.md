
Project Description (350 Characters)
This project implements a robust TFTP (Trivial File Transfer Protocol) system, featuring a server-client architecture. It supports commands like file upload/download (WRQ/RRQ), directory listing (DIRQ), file deletion (DELRQ), and broadcasting changes (BCAST). Built with Java, it emphasizes concurrency, error handling, and efficient communication.

README File
markdown
Copy code
# **TFTP Implementation**

## **Overview**
This project implements a fully functional **TFTP (Trivial File Transfer Protocol)** system using Java, showcasing a client-server architecture for efficient file transfer and management. It adheres to the TFTP specification while extending features for enhanced functionality.

---

## **Features**

### **Server-Side (TftpProtocol):**
- **File Transfer Commands:**
  - **RRQ (Read Request):** Retrieve files from the server.
  - **WRQ (Write Request):** Upload files to the server.
- **File Management:**
  - **DIRQ:** List available files on the server.
  - **DELRQ:** Delete specific files.
- **Broadcast Notifications:**
  - Uses **BCAST** packets to notify clients of file additions/deletions.
- **Error Handling:**
  - Comprehensive error codes for invalid requests, missing files, and permission issues.
- **Concurrency:** Supports multiple client connections efficiently using threads.

### **Client-Side (TftpClient):**
- **Interactive CLI:**
  - Execute commands like `LOGRQ`, `RRQ`, `WRQ`, `DIRQ`, and `DELRQ`.
- **File Management:**
  - Downloads and uploads files seamlessly.
  - Displays directory listings with `DIRQ`.
- **Error Feedback:**
  - Displays server error messages and responses for better user understanding.

---

## **Architecture**

### **Protocol Design**
- Implements TFTP opcodes: `RRQ`, `WRQ`, `DATA`, `ACK`, `ERROR`, `DIRQ`, `DELRQ`, `BCAST`, and `DISC`.
- Encodes and decodes messages using custom logic for packet formation and validation.
