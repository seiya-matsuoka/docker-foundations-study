import { useEffect, useState } from 'react';

// この値は Vite Development Server 起動時に Container Environment から読み込まれる。
// ただし fetch を実行するのは Frontend Container ではなく Host Browser 上の JavaScript である。
// そのため Docker Network 内の Service 名 backend ではなく、Browser から到達できる URL を指定する。
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
      <h1>Unit 11 - 3 Container</h1>

      <p>
        Browser → React → Spring Boot → PostgreSQL
        の通信経路を確認する学習用画面です。
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
