import { useEffect, useState } from 'react';

// Production Build 時に VITE_API_BASE_URL が Browser 用 JavaScript へ反映される。
// Frontend に公開される値なので Secret は含めない。
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

function App() {
  const [message, setMessage] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadMessage() {
      try {
        const response = await fetch(`${apiBaseUrl}/api/message`);

        if (!response.ok) {
          throw new Error(`API request failed: ${response.status}`);
        }

        const data = await response.json();
        setMessage(data);
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : 'API request failed',
        );
      }
    }

    loadMessage();
  }, []);

  return (
    <main className="app">
      <h1>Unit 12 - Docker Best Practices</h1>

      <p>
        Multi-stage Build / Healthcheck / non-root
        などを適用した学習用構成です。
      </p>

      <section>
        <h2>API 接続先</h2>
        <code>{apiBaseUrl}</code>
      </section>

      <section>
        <h2>PostgreSQL から取得した Data</h2>

        {message && (
          <p>
            ID: {message.id} / Message: {message.message}
          </p>
        )}

        {error && <p role="alert">Error: {error}</p>}

        {!message && !error && <p>Loading...</p>}
      </section>
    </main>
  );
}

export default App;
