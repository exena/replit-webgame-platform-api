package com.arcadex.api.config;

import com.arcadex.api.entity.Game;
import com.arcadex.api.repository.GameRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final GameRepository gameRepository;

    public DataSeeder(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public void run(String... args) {
        if (gameRepository.count() == 0) {
            System.out.println("Seeding database with initial games...");

            gameRepository.save(new Game(
                    "2048",
                    "Join the numbers and get to the 2048 tile! A classic puzzle game that tests your strategic thinking.",
                    "https://images.unsplash.com/photo-1614294148960-9aa740632a87?q=80&w=600&auto=format&fit=crop",
                    "https://funhtml5games.com/?play=2048bit",
                    "Puzzle"
            ));

            gameRepository.save(new Game(
                    "Hextris",
                    "An addictive puzzle game inspired by Tetris. Rotate the hexagon to clear lines!",
                    "https://images.unsplash.com/photo-1551103782-8ab07afd45c1?q=80&w=600&auto=format&fit=crop",
                    "https://hextris.io/",
                    "Arcade"
            ));

            gameRepository.save(new Game(
                    "Dinosaur Game",
                    "The classic Chrome offline dinosaur run game. Jump over cacti and dodge pterodactyls.",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=600&auto=format&fit=crop",
                    "https://chromedino.com/",
                    "Runner"
            ));

            gameRepository.save(new Game(
                    "Flappy Bird",
                    "Navigate the bird through the pipes. Simple to play, hard to master.",
                    "https://images.unsplash.com/photo-1579373903781-fd5c0c30c4cd?q=80&w=600&auto=format&fit=crop",
                    "https://flappybird.io/",
                    "Arcade"
            ));

            System.out.println("Seeding complete!");
        }
    }
}
