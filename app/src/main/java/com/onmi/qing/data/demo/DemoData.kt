package com.onmi.qing.data.demo

import com.onmi.qing.data.Achievement
import com.onmi.qing.data.AchievementList
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.UsageStats

// 演示模式预定义数据 - 包含丰富的场景数据用于演示
object DemoData {

    // 心理维度演示数据
    val psychologyDimensions = PsychologyDimensions(
        moodStability = 0.75f,
        selfAwareness = 0.68f,
        stressManagement = 0.52f,
        socialConfidence = 0.45f,
        sleepQuality = 0.58f,
        selfCare = 0.72f
    )

    // 使用统计演示数据
    val usageStats = UsageStats(
        chatCount = 15,
        breathingCount = 8,
        checkInCount = 12
    )

    // 聊天会话演示数据

    // 获取预定义的演示聊天会话列表
    fun getDemoSessions(): List<ChatSession> {
        return listOf(
            createSession1(),
            createSession2(),
            createSession3(),
            createSession4(),
            createSession5()
        )
    }

    // 场景1: 学习压力倾诉
    private fun createSession1(): ChatSession {
        val sessionId = "demo_session_1"
        val baseTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L // 2天前

        val messages = listOf(
            ChatMessage("msg_1_1", MessageRole.USER, listOf(MessagePart.Text("最近期末考试压力好大，感觉自己什么都记不住...")), baseTime),
            ChatMessage("msg_1_2", MessageRole.ASSISTANT, listOf(MessagePart.Text("我理解你的感受，考试前的紧张是很常见的。期末确实是一个压力比较大的时期。试着深呼吸几次，让自己先放松一下。你觉得目前最大的困扰是什么呢？是担心成绩，还是觉得复习时间不够？")), baseTime + 60000),
            ChatMessage("msg_1_3", MessageRole.USER, listOf(MessagePart.Text("主要是担心挂科...这门课内容太多了")), baseTime + 120000),
            ChatMessage("msg_1_4", MessageRole.ASSISTANT, listOf(MessagePart.Text("担心挂科是很正常的担忧。我建议你可以先把知识点分分类，优先复习最重要的部分。其实你愿意主动和我聊这些，说明你很积极地在面对呢。你有没有试过用思维导图来整理知识点？")), baseTime + 180000),
            ChatMessage("msg_1_5", MessageRole.USER, listOf(MessagePart.Text("没有试过，听起来不错")), baseTime + 240000)
        )

        return ChatSession(
            id = sessionId,
            title = "期末考试压力",
            lastMessage = "小晴: 没有试过，听起来不错",
            timestamp = baseTime + 240000,
            messageCount = 5,
            messages = messages
        )
    }

    // 场景2: 人际关系困惑
    private fun createSession2(): ChatSession {
        val sessionId = "demo_session_2"
        val baseTime = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L // 3天前

        val messages = listOf(
            ChatMessage("msg_2_1", MessageRole.USER, listOf(MessagePart.Text("我和室友相处不太愉快，总是因为一些小事情发生争执...")), baseTime),
            ChatMessage("msg_2_2", MessageRole.ASSISTANT, listOf(MessagePart.Text("宿舍关系确实是我们大学生活中很重要的一部分。你们之间有什么具体的矛盾吗？是作息时间不同，还是生活习惯的差异？")), baseTime + 60000),
            ChatMessage("msg_2_3", MessageRole.USER, listOf(MessagePart.Text("主要是作息时间，我习惯早睡，但他们经常熬夜到很晚")), baseTime + 120000),
            ChatMessage("msg_2_4", MessageRole.ASSISTANT, listOf(MessagePart.Text("原来是这样！作息时间的差异确实容易产生摩擦。我建议你可以找一个大家比较放松的时间，心平气和地沟通一下你的感受，也可以听听他们的想法。很多时候沟通是解决问题的第一步。你觉得呢？")), baseTime + 180000)
        )

        return ChatSession(
            id = sessionId,
            title = "宿舍关系困扰",
            lastMessage = "小晴: 很多时候沟通是解决问题的第一步...",
            timestamp = baseTime + 180000,
            messageCount = 4,
            messages = messages
        )
    }

    // 场景3: 睡前放松对话
    private fun createSession3(): ChatSession {
        val sessionId = "demo_session_3"
        val baseTime = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L // 昨天

        val messages = listOf(
            ChatMessage("msg_3_1", MessageRole.USER, listOf(MessagePart.Text("睡不着，能陪我聊聊天吗？")), baseTime),
            ChatMessage("msg_3_2", MessageRole.ASSISTANT, listOf(MessagePart.Text("当然可以呀！我在这里陪你。今天发生了什么事情让你睡不着吗？")), baseTime + 60000),
            ChatMessage("msg_3_3", MessageRole.USER, listOf(MessagePart.Text("脑子里一直在想明天要做演讲，好紧张")), baseTime + 120000),
            ChatMessage("msg_3_4", MessageRole.ASSISTANT, listOf(MessagePart.Text("演讲前的紧张是很正常的呢。你知道吗，很多人都会这样，适度的紧张反而能帮助我们发挥得更好。不如我们来做几个深呼吸，放松一下身心？")), baseTime + 180000),
            ChatMessage("msg_3_5", MessageRole.USER, listOf(MessagePart.Text("好的，怎么做呢？")), baseTime + 240000),
            ChatMessage("msg_3_6", MessageRole.ASSISTANT, listOf(MessagePart.Text("试试4-7-8呼吸法：用鼻子吸气4秒，屏住呼吸7秒，然后用嘴缓缓呼气8秒。重复几次，你会感觉放松很多。你平时睡前会做些什么来帮助入睡呢？")), baseTime + 300000)
        )

        return ChatSession(
            id = sessionId,
            title = "睡前放松",
            lastMessage = "小晴: 你平时睡前会做些什么来帮助入睡呢？",
            timestamp = baseTime + 300000,
            messageCount = 6,
            messages = messages
        )
    }

    // 场景4: 情绪低落支持
    private fun createSession4(): ChatSession {
        val sessionId = "demo_session_4"
        val baseTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L // 5天前

        val messages = listOf(
            ChatMessage("msg_4_1", MessageRole.USER, listOf(MessagePart.Text("今天感觉特别低落，对什么都提不起兴趣...")), baseTime),
            ChatMessage("msg_4_2", MessageRole.ASSISTANT, listOf(MessagePart.Text("我在这里陪你。想说什么都可以，我听着呢。这种低落的情绪大概持续多久了？有什么特别的原因吗？")), baseTime + 60000),
            ChatMessage("msg_4_3", MessageRole.USER, listOf(MessagePart.Text("可能是因为最近没什么成就感吧，感觉自己什么都不行")), baseTime + 120000),
            ChatMessage("msg_4_4", MessageRole.ASSISTANT, listOf(MessagePart.Text("我能理解这种感觉。其实每个人都会有低落的时候，这不代表你真的什么都不行。你能和我分享一件最近让你觉得还不错的小事吗？即使是微不足道的事情也可以。")), baseTime + 180000),
            ChatMessage("msg_4_5", MessageRole.USER, listOf(MessagePart.Text("上周帮助了一个迷路的同学找到教室")), baseTime + 240000)
        )

        return ChatSession(
            id = sessionId,
            title = "情绪低落",
            lastMessage = "我: 上周帮助了一个迷路的同学找到教室",
            timestamp = baseTime + 240000,
            messageCount = 5,
            messages = messages
        )
    }

    // 场景5: 自我成长探讨
    private fun createSession5(): ChatSession {
        val sessionId = "demo_session_5"
        val baseTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L // 7天前

        val messages = listOf(
            ChatMessage("msg_5_1", MessageRole.USER, listOf(MessagePart.Text("我想成为更好的自己，你觉得我应该从哪些方面努力？")), baseTime),
            ChatMessage("msg_5_2", MessageRole.ASSISTANT, listOf(MessagePart.Text("这是一个很棒的问题！自我成长是一个持续的过程。你已经有意识地想要提升自己，这本身就是一个很好的开始。你觉得目前最想改善的是哪个方面呢？比如人际关系、学习能力，还是情绪管理？")), baseTime + 60000),
            ChatMessage("msg_5_3", MessageRole.USER, listOf(MessagePart.Text("情绪管理吧，我感觉自己经常容易焦虑")), baseTime + 120000),
            ChatMessage("msg_5_4", MessageRole.ASSISTANT, listOf(MessagePart.Text("觉察到自己的情绪模式是改变的第一步！对于情绪管理，我建议可以尝试：1）每天留几分钟冥想或深呼吸；2）记录情绪日记，帮助自己理清情绪来源；3）适度运动，释放压力。你觉得哪个方法比较适合你？")), baseTime + 180000)
        )

        return ChatSession(
            id = sessionId,
            title = "自我成长",
            lastMessage = "小晴: 你觉得哪个方法比较适合你？",
            timestamp = baseTime + 180000,
            messageCount = 4,
            messages = messages
        )
    }

    // 心情记录演示数据

    fun getDemoMoodEntries(): List<MoodEntry> {
        val now = System.currentTimeMillis()
        return listOf(
            MoodEntry(
                id = "mood_demo_1",
                mood = MoodType.HAPPY,
                reason = "期末考试终于结束了，感觉轻松多了！虽然不知道成绩怎样，但总算熬过来了～",
                timestamp = now - 1 * 24 * 60 * 60 * 1000L
            ),
            MoodEntry(
                id = "mood_demo_2",
                mood = MoodType.CALM,
                reason = "今天完成了所有作业，晚上还去操场跑了步，心情很平静。",
                timestamp = now - 2 * 24 * 60 * 60 * 1000L
            ),
            MoodEntry(
                id = "mood_demo_3",
                mood = MoodType.UNHAPPY,
                reason = "和同学因为小组作业的事情有些争执，有点不开心。希望明天能好好沟通解决。",
                timestamp = now - 3 * 24 * 60 * 60 * 1000L
            ),
            MoodEntry(
                id = "mood_demo_4",
                mood = MoodType.CALM,
                reason = "昨晚睡得很好，今天一整天都精力充沛。继续保持早睡早起的好习惯！",
                timestamp = now - 4 * 24 * 60 * 60 * 1000L
            ),
            MoodEntry(
                id = "mood_demo_5",
                mood = MoodType.HAPPY,
                reason = "今天和好久不见的朋友视频聊天，分享了最近的大学生活，很开心！",
                timestamp = now - 5 * 24 * 60 * 60 * 1000L
            )
        )
    }

    // 成就演示数据

    // 获取已解锁部分成就的演示数据
    fun getDemoAchievements(): List<Achievement> {
        val unlockedIds = setOf(
            "checkin_3",      // 初次签到
            "checkin_7",      // 一周坚持
            "chat_10",        // 初次倾诉
            "breathing_5",    // 呼吸初体验
            "mood_good_7",    // 情绪稳定
            "perfect_day"    // 完美一天
        )

        return AchievementList.achievements.map { achievement ->
            if (achievement.id in unlockedIds) {
                achievement.copy(
                    isUnlocked = true,
                    unlockedDate = "2026/03/25"
                )
            } else {
                achievement
            }
        }
    }

    // 用户信息演示数据

    val demoUserName = "小明同学"
    val demoUserDescription = "正在使用小晴心理健康助手"
}
