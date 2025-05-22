# OpenAI ChatGPT Spring Boot Application

## Overview

This application is a Spring Boot service that connects to OpenAI's Chat API (specifically using models like GPT-3.5-turbo). It provides a simple REST endpoint to send a message to the OpenAI API and receive a chat completion response.

The application now supports **Retrieval Augmented Generation (RAG)** to provide more contextually relevant answers. If RAG is enabled, the application will first retrieve relevant information from an ingested knowledge base (currently an in-memory vector store with sample data) and then use this information to augment the prompt sent to the OpenAI language model.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java JDK:** Version 17 or newer.
*   **Apache Maven:** For building and managing the project.
*   **OpenAI API Key:** An active API key from OpenAI to authenticate requests. This is crucial for both chat completions and generating embeddings for RAG.

## Setup & Configuration

1.  **Clone the Repository:**
    ```bash
    git clone <repository_url>
    cd chatgpt-app
    ```
    (Replace `<repository_url>` with the actual URL of this repository if applicable, otherwise download the source code.)

2.  **Configure OpenAI API Key and Other Settings:**
    The application requires your OpenAI API key and has other configurable settings.
    *   Open the file: `src/main/resources/application.properties`
    *   **OpenAI API Key (Mandatory):**
        *   Find or add the line for `openai.api.key` and replace `YOUR_ACTUAL_API_KEY` with your actual OpenAI API key:
            ```properties
            openai.api.key=YOUR_ACTUAL_API_KEY
            ```
        *   **Important:** Keep your API key secure and do not commit it to public repositories.
    *   **RAG and Data Ingestion Settings (Optional - Defaults Provided):**
        *   See the "RAG Features & Configuration" section below for details on these properties. You can modify them in `application.properties` if needed.

## RAG Features & Configuration

Retrieval Augmented Generation (RAG) enhances the language model's responses by providing it with relevant context retrieved from a knowledge base. This application uses an in-memory vector store for this purpose.

The following properties in `src/main/resources/application.properties` control RAG and data ingestion behavior:

*   `rag.enabled` (boolean): Enables or disables the RAG functionality.
    *   Default: `true`
*   `rag.topk` (int): Specifies the number of most relevant text chunks to retrieve from the vector store to use as context.
    *   Default: `3`
*   `openai.embedding.model` (String): The OpenAI model used for generating text embeddings (vector representations of text) for both ingested documents and user queries.
    *   Default: `text-embedding-3-small`
*   `data.ingestion.chunkSize` (int): The size (in characters) into which documents are split during the ingestion process.
    *   Default: `1000`
*   `data.ingestion.chunkOverlap` (int): The number of characters that overlap between consecutive text chunks during ingestion. This helps maintain context between chunks.
    *   Default: `200`

## Data Ingestion

For the RAG functionality to be effective, relevant data must be ingested into the application's vector store. This process involves:
1.  Splitting documents into smaller text chunks.
2.  Generating embeddings (vector representations) for each chunk using the configured OpenAI embedding model.
3.  Storing these chunks and their embeddings in the vector store.

**Sample Data Ingestion:**
Currently, the application includes a sample data ingestion mechanism:
*   The `DataIngestionService` contains a method annotated with `@PostConstruct` called `sampleIngestion()`. This method is automatically executed when the application starts up.
*   It loads two sample text documents into the in-memory vector store:
    1.  A general text about "The quick brown fox..." which also mentions OpenAI and its language models (e.g., GPT-3, GPT-4).
    2.  A text describing "Spring Boot" as a popular framework for Java applications.

This sample data allows you to test the RAG functionality out-of-the-box by asking questions related to these topics.

**(Future Possibility):** For custom data ingestion, future versions might include an API endpoint for submitting documents or instructions for loading local files. For now, developers can modify the `DataIngestionService#sampleIngestion()` method to include different or additional texts.

## Build

To build the project and package it, navigate to the project's root directory (where `pom.xml` is located) and run the following Maven command:

```bash
mvn clean install
```
This command will compile the code, run tests, and create a JAR file in the `target/` directory.

## Run

There are a couple of ways to run the application:

1.  **Using Maven Spring Boot Plugin (Recommended for Development):**
    ```bash
    mvn spring-boot:run
    ```
    The application will start, and by default, it will be accessible on `http://localhost:8080`. During startup, you will see logs related to the sample data ingestion.

2.  **Running the Packaged JAR:**
    After building the project, you can run the generated JAR file:
    ```bash
    java -jar target/chatgpt-app-0.0.1-SNAPSHOT.jar
    ```
    *Note: The JAR file name might change depending on the project version defined in the `pom.xml`.*

## Usage/Endpoints

The application exposes a single REST endpoint for chat interactions.

*   **Endpoint:** `/api/v1/chat`
*   **Method:** `POST`
*   **Request Body:** Plain text message representing the user's input.
*   **Content-Type:** `text/plain`

*   **Example using `curl`:**
    ```bash
    curl -X POST -H "Content-Type: text/plain" -d "What is Spring Boot?" http://localhost:8080/api/v1/chat
    ```

*   **Successful Response:**
    The API will return the AI's chat message as a plain text string.
    *   If RAG is enabled (default) and relevant context is found in the ingested documents, the response will be informed by this context. For example, asking "What is Spring Boot?" should yield a response based on the sample data.
    *   If RAG is disabled or no relevant context is found, the response will be a direct answer from the language model based on its general knowledge.
    Example: (If context on Spring Boot is found) `Spring Boot is a popular framework for building Java applications that simplifies the development of stand-alone, production-grade Spring based Applications.`

*   **Error Responses:**
    If there's an issue (e.g., API key invalid, OpenAI service error, network problem), the response will be a plain text error message.
    Examples:
    *   `Error: OpenAI API Key is not configured. Please contact the administrator.`
    *   `Error communicating with OpenAI: 401 UNAUTHORIZED - ...`
    *   `Error: No response from OpenAI or response was empty.`

## Project Structure

The project follows a standard Spring Boot application structure:

*   **`src/main/java/com/example/chatgptapp`**: Main package root.
    *   **`ChatgptAppApplication.java`**: The main Spring Boot application class.
    *   **`config`**: Contains configuration classes (e.g., `RestTemplateConfig`).
    *   **`controller`**: Contains Spring MVC controllers (`ChatController`).
    *   **`dto`**: Data Transfer Objects (e.g., `OpenAIChatRequest`, `OpenAIEmbeddingRequest`, `VectorData`).
    *   **`service`**: Contains business logic:
        *   `OpenAIService`: Handles interaction with OpenAI APIs (chat and embeddings), includes RAG logic.
        *   `DataIngestionService`: Manages the process of chunking, embedding, and storing text data.
        *   `VectorStoreService`: Interface for vector storage operations.
        *   `vectorstore/InMemoryVectorStoreService.java`: In-memory implementation of `VectorStoreService`.
    *   **`util`**: Utility classes (e.g., `TextChunker`).
*   **`src/main/resources`**:
    *   **`application.properties`**: Configuration file for Spring Boot application properties, including OpenAI keys and RAG settings.
    *   **`static`**: For static web resources (currently empty).
    *   **`templates`**: For server-side templates (currently empty).
*   **`src/test/java`**: Contains unit tests for various components.
*   **`pom.xml`**: Maven project object model file.

---
This README provides a comprehensive guide for users to set up, configure, build, run, and use the application, including its RAG capabilities.
