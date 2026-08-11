# WebSocket-Based Gomoku Battle Platform

An online Gomoku battle platform implemented based on Spring Boot and WebSocket, supporting features such as user matching, battles, and point rankings.

## Project Introduction

This project is a complete online Gomoku battle system adopting a separated frontend-backend architecture. The frontend provides a friendly Web interface, while the backend is built on the Spring Boot framework using WebSocket for real-time two-way communication. The system supports user registration and login, a three-level matching mechanism (Bronze/Silver/Gold), real-time battles, win/loss judgment, and point statistics.

## Tech Stack

Backend Technology: Spring Boot, Spring WebSocket, MyBatis, Lombok
Frontend Technology: HTML5, CSS3, JavaScript, jQuery
Database: MySQL
Build Tool: Maven

## Features

The user module supports account registration, login, and basic information management. After logging in, users can enter the game lobby to view personal point data. The battle module utilizes a three-level matching pool design, automatically matching opponents of similar strength based on user ranks. Upon successful matching, players enter a room for real-time battles. Winning or losing is automatically judged after moves are made, and points are updated. All communication is implemented through WebSocket long connections to ensure a smooth and lag-free gaming experience.

## Project Structure

Core code is located in the `src/main/java/org/gobang_battle` directory. The `config` directory contains WebSocket configurations, `controller` handles WebSocket requests and HTTP interfaces, `game` encapsulates game-related request/response objects, `mapper` implements data access, `model` defines core business models such as Room and RoomManager, and `service` provides user business logic. Static resources (HTML, CSS, images) are stored in the `src/main/resources/static` directory.

## Environment Requirements

JDK 8 or higher
Maven 3.x
MySQL 5.7 or higher

## Quick Start

After cloning the project, create the database and import the SQL script. Modify the database connection configuration in `application.yml`. Execute the `mvn clean package` command to package, then run `java -jar target/*.jar` to start the service. Access `http://localhost:8080` to enter the game interface.

## WebSocket Message Format

Match Request: `{"message":"start_match"}`
Match Response: `{"ok":true,"reason":"Match successful","message":"matched"}`
Game Ready: `{"message":"game_ready","roomId":"xxx","thisUserId":1,"thatUserId":2,"whiteUser":1}`
Move Request: `{"message":"move","userId":1,"row":7,"col":8}`
Move Response: `{"message":"move","userId":1,"row":7,"col":8,"win":0}`

## License

This project is for learning and communication purposes only.