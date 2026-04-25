package movie

import (
	"compress/gzip"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"os"
	"path/filepath"
	"strings"
	"time"

	"gabe565.com/ascii-movie/movies"
	"gabe565.com/ascii-movie/internal/progressbar"
	"gabe565.com/utils/slogx"
	"github.com/charmbracelet/lipgloss"
)

var ErrInvalidSpeed = errors.New("speed must be greater than 0")

type movieManifest struct {
	FramesDir  string `json:"framesDir"`
	FrameCount int    `json:"frameCount"`
	Width      int    `json:"width"`
	Frames     []struct {
		File       string `json:"file"`
		DurationMs int64  `json:"durationMs"`
	} `json:"frames"`
}

func Load(path string, speed float64) (Movie, error) {
	var err error

	slog.Info("Loading movie...")
	start := time.Now()

	movie := NewMovie()

	if path != "" {
		if stat, statErr := os.Stat(path); statErr == nil {
			if stat.IsDir() {
				return loadMovieFromManifest(filepath.Join(path, "manifest.json"), speed)
			}
			if strings.EqualFold(filepath.Ext(path), ".json") {
				return loadMovieFromManifest(path, speed)
			}
		} else if errors.Is(statErr, os.ErrNotExist) && strings.EqualFold(filepath.Ext(path), ".json") {
			return movie, statErr
		} else if statErr != nil {
			return movie, statErr
		}
	}

	var src io.ReadCloser
	if path == "" {
		// Use default embedded movie
		path = movies.Default
	}
	// Load embedded movie
	embeddedPath := path
	if !strings.HasSuffix(embeddedPath, FileSuffix) {
		embeddedPath += FileSuffix
	}
	if src, err = movies.Movies.Open(embeddedPath); err == nil {
		slog.Debug("Using embedded movie", "name", embeddedPath)

		if strings.HasSuffix(embeddedPath, ".gz") {
			src, err = gzip.NewReader(src)
			if err != nil {
				return movie, err
			}
		}
	} else {
		if errors.Is(err, os.ErrNotExist) {
			// Fallback to loading file
			slogx.Trace("No embedded movie matches name. Searching filesystem.")
			f, err := os.Open(path)
			if err != nil {
				return movie, err
			}
			slog.Debug("Found movie file", "name", path)

			src = f
			defer func(f *os.File) {
				_ = f.Close()
			}(f)

			if strings.HasSuffix(path, ".gz") {
				src, err = gzip.NewReader(src)
				if err != nil {
					return movie, err
				}
			}
		} else {
			return movie, err
		}
	}

	if speed <= 0 {
		return movie, fmt.Errorf("%w: %g", ErrInvalidSpeed, speed)
	}

	if err := movie.LoadFile(path, src, speed); err != nil {
		return movie, err
	}

	if err := src.Close(); err != nil {
		return movie, err
	}

	slog.Info("Movie loaded",
		"name", movie.Filename,
		"frames", len(movie.Frames),
		"duration", movie.Duration().Round(time.Second),
		"took", time.Since(start).Round(time.Microsecond),
	)

	return movie, nil
}

func loadMovieFromManifest(manifestPath string, speed float64) (Movie, error) {
	var movie Movie
	if speed <= 0 {
		return movie, fmt.Errorf("%w: %g", ErrInvalidSpeed, speed)
	}

	manifestBytes, err := os.ReadFile(manifestPath)
	if err != nil {
		return movie, err
	}

	var manifest movieManifest
	if err := json.Unmarshal(manifestBytes, &manifest); err != nil {
		return movie, err
	}

	framesDir := manifest.FramesDir
	if framesDir == "" {
		framesDir = filepath.Dir(manifestPath)
	}
	if !filepath.IsAbs(framesDir) {
		framesDir = filepath.Join(filepath.Dir(manifestPath), framesDir)
	}

	movie.Filename = filepath.Base(manifestPath)
	movie.Frames = make([]Frame, 0, len(manifest.Frames))

	maxWidth := manifest.Width
	maxHeight := 0
	for _, frameInfo := range manifest.Frames {
		framePath := filepath.Join(framesDir, frameInfo.File)
		frameBytes, err := os.ReadFile(framePath)
		if err != nil {
			return movie, err
		}

		data := strings.TrimSuffix(string(frameBytes), "\n")
		duration := time.Duration(frameInfo.DurationMs) * time.Millisecond
		if duration <= 0 {
			duration = time.Second / 15
		}
		duration = time.Duration(float64(duration) / speed)

		movie.Frames = append(movie.Frames, Frame{Duration: duration, Data: data})

		if w := lipgloss.Width(data); w > maxWidth {
			maxWidth = w
		}
		if h := lipgloss.Height(data); h > maxHeight {
			maxHeight = h
		}
	}

	movie.Width = maxWidth
	movie.Height = maxHeight

	if len(movie.Frames) == 0 {
		return movie, fmt.Errorf("manifest does not contain any frames: %s", manifestPath)
	}

	bar := progressbar.New()
	totalDuration := movie.Duration()
	movie.Sections = make([]int, movie.Width+1)
	currentPosition := time.Duration(0)
	for i, frame := range movie.Frames {
		movie.Frames[i].Progress = bar.Generate(currentPosition+frame.Duration/2, totalDuration, movie.Width+2)
		percent := int(currentPosition * time.Duration(movie.Width) / totalDuration)
		if percent < len(movie.Sections)-1 {
			movie.Sections[percent+1] = i
		}
		currentPosition += frame.Duration
	}

	return movie, nil
}
