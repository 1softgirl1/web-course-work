import { createReadStream, existsSync, statSync } from 'node:fs'
import { createServer, request } from 'node:http'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(fileURLToPath(new URL('.', import.meta.url)), 'dist')
const port = Number(process.env.PORT || 80)
const backendUrl = new URL(process.env.BACKEND_URL || 'http://backend:8080')
const proxyPrefixes = ['/api/', '/auth/', '/v3/api-docs/', '/swagger-ui/']
const proxyExactPaths = new Set(['/api', '/auth', '/v3/api-docs', '/swagger-ui', '/swagger-ui.html'])

const contentTypes = new Map([
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'application/javascript; charset=utf-8'],
  ['.css', 'text/css; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.svg', 'image/svg+xml'],
  ['.png', 'image/png'],
  ['.jpg', 'image/jpeg'],
  ['.jpeg', 'image/jpeg'],
  ['.ico', 'image/x-icon'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2'],
])

const shouldProxy = (pathname) =>
  proxyExactPaths.has(pathname) || proxyPrefixes.some((prefix) => pathname.startsWith(prefix))

const proxy = (clientReq, clientRes) => {
  const target = new URL(clientReq.url || '/', backendUrl)
  const headers = { ...clientReq.headers, host: backendUrl.host }

  const proxyReq = request(
    {
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port,
      path: `${target.pathname}${target.search}`,
      method: clientReq.method,
      headers,
    },
    (proxyRes) => {
      clientRes.writeHead(proxyRes.statusCode || 502, proxyRes.headers)
      proxyRes.pipe(clientRes)
    },
  )

  proxyReq.on('error', (error) => {
    clientRes.writeHead(502, { 'content-type': 'application/json; charset=utf-8' })
    clientRes.end(JSON.stringify({ message: 'Backend proxy error', details: [error.message] }))
  })

  clientReq.pipe(proxyReq)
}

const sendFile = (res, filePath) => {
  const extension = extname(filePath)
  res.writeHead(200, { 'content-type': contentTypes.get(extension) || 'application/octet-stream' })
  createReadStream(filePath).pipe(res)
}

const serveStatic = (req, res) => {
  const url = new URL(req.url || '/', 'http://localhost')
  let pathname = decodeURIComponent(url.pathname)

  if (pathname === '/') {
    res.writeHead(302, { location: '/web-course-work/' })
    res.end()
    return
  }

  if (pathname === '/web-course-work') {
    res.writeHead(302, { location: '/web-course-work/' })
    res.end()
    return
  }

  if (pathname.startsWith('/web-course-work/')) {
    pathname = pathname.slice('/web-course-work'.length)
  }

  if (pathname.endsWith('/')) pathname += 'index.html'

  const candidate = normalize(join(root, pathname))
  const fallback = join(root, 'index.html')
  const insideRoot = candidate.startsWith(root)

  if (insideRoot && existsSync(candidate) && statSync(candidate).isFile()) {
    sendFile(res, candidate)
    return
  }

  if (existsSync(fallback)) {
    sendFile(res, fallback)
    return
  }

  res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' })
  res.end('Not found')
}

createServer((req, res) => {
  const pathname = new URL(req.url || '/', 'http://localhost').pathname
  if (shouldProxy(pathname)) {
    proxy(req, res)
    return
  }

  serveStatic(req, res)
}).listen(port, '0.0.0.0', () => {
  console.log(`Frontend server listening on ${port}, proxying API to ${backendUrl}`)
})
