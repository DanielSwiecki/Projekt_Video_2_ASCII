package movie

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestFromFlags(t *testing.T) {
	t.Run("default embedded", func(t *testing.T) {
		t.Parallel()
		_, err := Load("", 1)
		require.NoError(t, err)
	})

	t.Run("short_intro embedded", func(t *testing.T) {
		t.Parallel()
		_, err := Load("short_intro", 1)
		require.NoError(t, err)
	})

	t.Run("short_intro file", func(t *testing.T) {
		t.Parallel()
		_, err := Load("../../movies/short_intro.txt", 1)
		require.NoError(t, err)
	})

	t.Run("invalid speed", func(t *testing.T) {
		t.Parallel()
		_, err := Load("", -1)
		require.Error(t, err)
	})

	t.Run("manifest directory", func(t *testing.T) {
			t.Parallel()
			tempDir := t.TempDir()
			framePath := filepath.Join(tempDir, "frame_00000.txt")
			require.NoError(t, os.WriteFile(framePath, []byte("frame one\n"), 0o644))
			manifest := `{
  "framesDir": ".",
  "frameCount": 1,
  "width": 9,
  "frames": [
    { "file": "frame_00000.txt", "durationMs": 100 }
  ]
}`
			require.NoError(t, os.WriteFile(filepath.Join(tempDir, "manifest.json"), []byte(manifest), 0o644))

			movie, err := Load(tempDir, 1)
			require.NoError(t, err)
			require.Equal(t, 1, len(movie.Frames))
			require.Equal(t, "frame one", movie.Frames[0].Data)
	})
