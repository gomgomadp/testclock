# testclock


feature

add -  Coroutine + lifecycleScope


import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

override fun onResume() {
    super.onResume()
    clockJob = lifecycleScope.launch {
        while (isActive) {
            val now = System.currentTimeMillis()
            val currentTime = timeFormat.format(Date(now))
            val currentDate = dateFormat.format(Date(now))

            clockView.text = currentTime
            dateView.text = currentDate

            val nextTick = now + 1000 - (now % 1000)
            val delay = nextTick - System.currentTimeMillis()
            delay(delay)
        }
    }
}

override fun onPause() {
    super.onPause()
    clockJob?.cancel()
}

private var clockJob: Job? = null

띄운 뒤에 설정값을 저장하거나 UI에 반영하는 흐름도 추가