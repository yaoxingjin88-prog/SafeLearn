import { useRouter } from 'vue-router'
import { useAppBase } from './useAppBase'

/**
 * 课程模块导航：避免课程/章节来回切换时历史栈无限堆积。
 * - 返回：固定回到上级（列表 / 课程详情），一次即可
 * - 横向切换课程、章节内翻页：使用 replace
 */
export function useCourseNavigation() {
  const router = useRouter()
  const { p } = useAppBase()

  function backToCourseList() {
    return router.replace(p('/courses/list'))
  }

  function backToCourseDetail(courseId: string) {
    return router.replace(p(`/courses/${courseId}`))
  }

  function goToCourse(courseId: string, replace = false) {
    const path = p(`/courses/${courseId}`)
    return replace ? router.replace(path) : router.push(path)
  }

  /** 课程详情内切换相关课程 */
  function switchCourse(courseId: string) {
    return router.replace(p(`/courses/${courseId}`))
  }

  function goToChapter(courseId: string, chapterId: string, replace = false) {
    const path = p(`/courses/${courseId}/chapters/${chapterId}`)
    return replace ? router.replace(path) : router.push(path)
  }

  /** 章节目录 / 上一章 / 下一章 */
  function switchChapter(courseId: string, chapterId: string) {
    return router.replace(p(`/courses/${courseId}/chapters/${chapterId}`))
  }

  return {
    backToCourseList,
    backToCourseDetail,
    goToCourse,
    switchCourse,
    goToChapter,
    switchChapter,
  }
}
