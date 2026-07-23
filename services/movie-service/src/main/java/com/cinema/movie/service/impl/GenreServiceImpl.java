package com.cinema.movie.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.movie.dto.request.CreateGenreRequest;
import com.cinema.movie.dto.request.UpdateGenreRequest;
import com.cinema.movie.dto.response.GenreResponse;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.mapper.GenreMapper;
import com.cinema.movie.repository.GenreRepository;
import com.cinema.movie.service.GenreService;

@Service
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public GenreServiceImpl(
            GenreRepository genreRepository,
            GenreMapper genreMapper) {
        this.genreRepository = genreRepository;
        this.genreMapper = genreMapper;
    }

    @Override
    @Transactional
    public GenreResponse create(CreateGenreRequest request) {
        String normalizedName = request.name().trim();

        if (genreRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException(MovieErrorCode.GENRE_NOT_FOUND);
        }

        Genre genre = genreMapper.toEntity(request);
        genre.setName(normalizedName);

        Genre savedGenre = genreRepository.save(genre);

        return genreMapper.toResponse(savedGenre);
    }

    @Override
    public GenreResponse findById(UUID id) {
        return genreMapper.toResponse(findGenre(id));
    }

    @Override
    public List<GenreResponse> findAll() {
        return genreRepository.findAll()
                .stream()
                .map(genreMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GenreResponse update(
            UUID id,
            UpdateGenreRequest request) {
        Genre genre = findGenre(id);
        String normalizedName = request.name().trim();

        if (genreRepository.existsByNameIgnoreCaseAndIdNot(
                normalizedName,
                id)) {
            throw new ConflictException(MovieErrorCode.GENRE_ALREADY_EXISTS);
        }

        genreMapper.updateEntity(request, genre);
        genre.setName(normalizedName);

        return genreMapper.toResponse(genre);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Genre genre = findGenre(id);

        genreRepository.delete(genre);
    }

    private Genre findGenre(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.GENRE_NOT_FOUND));
    }
}
