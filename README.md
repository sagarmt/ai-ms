# OpenAI ChatGPT Spring Boot Application

## Overview

This application is a Spring Boot service that connects to OpenAI's Chat API (specifically using models like GPT-3.5-turbo). It provides a simple REST endpoint to send a message to the OpenAI API and receive a chat completion response.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java JDK:** Version 17 or newer.
*   **Apache Maven:** For building and managing the project.
*   **OpenAI API Key:** An active API key from OpenAI to authenticate requests.

## Setup & Configuration

1.  **Clone the Repository:**
    ```bash
    git clone <repository_url>
    cd chatgpt-app
    ```
    (Replace `<repository_url>` with the actual URL of this repository if applicable, otherwise download the source code.)

2.  **Configure OpenAI API Key:**
    The application requires your OpenAI API key to communicate with the OpenAI service.
    *   Open the file: `src/main/resources/application.properties`
    *   Find or add the line for `openai.api.key` and replace `YOUR_ACTUAL_API_KEY` with your actual OpenAI API key:
        ```properties
        openai.api.key=YOUR_ACTUAL_API_KEY
        ```
    *   **Important:** Keep your API key secure and do not commit it to public repositories.

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
    The application will start, and by default, it will be accessible on `http://localhost:8080`.

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
    curl -X POST -H "Content-Type: text/plain" -d "Hello, who are you?" http://localhost:8080/api/v1/chat
    ```

*   **Successful Response:**
    The API will return the AI's chat message as a plain text string.
    Example: `Hello! I am an AI language model.`

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
    *   **`config`**: Contains configuration classes (e.g., `RestTemplateConfig` for `RestTemplate` bean).
    *   **`controller`**: Contains Spring MVC controllers (`ChatController`) that handle incoming HTTP requests.
    *   **`dto`**: Data Transfer Objects used for structuring requests and responses to the OpenAI API (e.g., `OpenAIChatRequest`, `OpenAIChatResponse`, `MessageDto`).
    *   **`service`**: Contains business logic and service layer classes (`OpenAIService`) that interact with external services like OpenAI.
*   **`src/main/resources`**:
    *   **`application.properties`**: Configuration file for Spring Boot application properties, including the `openai.api.key`.
    *   **`static`**: For static web resources (currently empty).
    *   **`templates`**: For server-side templates (currently empty).
*   **`src/test/java`**: Contains unit and integration tests.
*   **`pom.xml`**: Maven project object model file, defining project dependencies and build configuration.

---
This README provides a comprehensive guide for users to set up, configure, build, run, and use the application.The `README.md` file has been created in the root of the project.
Let's verify its content.
