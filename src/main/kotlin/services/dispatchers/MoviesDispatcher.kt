package services.dispatchers

import GoogleService
import com.gitlab.kordlib.common.entity.DiscordMessage
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import data.Movie
import db.GuildsManager
import utils.Commands
import utils.commandRegex
import utils.setDefaultStatus
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class MoviesDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord
) : BaseDispatcher(client), MessageCreateListener, ReactionAddListener {

    private val googleService by lazy {
        GoogleService()
    }

    override val dispatcherPrefix: String
        get() = "movies"

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    private suspend fun getMovieList(moviesChannelId: String): Map<Int, List<DiscordMessage>> {
        val messages = client.rest.channel.getMessages(moviesChannelId, limit = 100)
        return messages
            .groupBy { it.reactions?.firstOrNull { reaction -> reaction.emoji.name == "\uD83D\uDC4D" }?.count ?: 0 }
    }

    private suspend fun showMoviesToRoll(channelId: String, moviesChannelId: String) {
        getMovieList(moviesChannelId)
            .maxByOrNull { it.key }
            ?.let { entry ->
                val newText = entry.value.mapIndexed { index, it ->
                    "${index + 1}. ${it.content} от ${getMention(it.author)}"
                }.joinToString("\n")
                val votesCount = entry.value.first().reactions?.firstOrNull { it.emoji.name == "\uD83D\uDC4D" }?.count ?: 0
                postMessage(channelId, "Фильмы с наибольшим количесвом лайков ($votesCount лайка у каждого):\n$newText")
            } ?: postErrorMessage(channelId, "Нечего смотреть")
    }

    private suspend fun rollMovies(channel: MessageChannelBehavior, moviesChannelId: String, withSearch: Boolean = false) {
        getMovieList(moviesChannelId)
            .maxByOrNull { it.key }
            ?.value?.randomOrNull()
            ?.let {
                postMessage(channel, "**${it.content}** от ${getMention(it.author)} :trophy:")
                if (withSearch) {
                    searchForMovie(channel, it.content)
                }
            } ?: postErrorMessage(channel, "Нечего смотреть")
    }

    private suspend fun getMovie(name: String): Pair<String, Movie?>? {
        return googleService.load(name)?.items?.firstOrNull {
            it.pageMap.movies?.firstOrNull() != null
        }?.let {
            val link = it.link
            val movie = it.pageMap.movies?.firstOrNull()
            link to movie
        }
    }

    private suspend fun searchForMovie(channel: MessageChannelBehavior, movieName: String) {
        if (movieName.isBlank()) {
            postErrorMessage(channel, "Не могу искать, если ты не скажешь, что искать")
            return
        }
        val (movieLink, movie) = getMovie(movieName) ?: run {
            postErrorMessage(channel, "Не нашёл")
            return
        }
        channel.createEmbed {
            color = Color.MAGENTA
            title = movie?.name ?: movieName
            author {
                icon = "https://yt3.ggpht.com/a/AATXAJz7FyhOdugVDwiazqdVf0P-xD1GOlkj-qdwD7cPtg=s900-c-k-c0xffffffff-no-rj-mo"
                name = "Смотреть на Кинопоиске"
                url = movieLink
            }
            movie?.image?.let { image = it }
            movie?.actors?.let {
                field {
                    name = "Актёры"
                    value = it
                    inline = true
                }
            }
            movie?.dateCreated?.convertDate()?.let {
                field {
                    name = "Дата выхода"
                    value = it
                    inline = true
                }
            }
            movie?.director?.let {
                field {
                    name = "Режиссёр"
                    value = it
                    inline = true
                }
            }
            movie?.genre?.let {
                field {
                    name = "Жанр"
                    value = it
                    inline = true
                }
            }
            movie?.description?.let {
                footer {
                    text = it
                }
            }
        }
    }

    private val top = Commands.Movies.TOP.commandRegex()
    private val help = Commands.Movies.HELP.commandRegex()
    private val set = Commands.Movies.SET.commandRegex()
    private val unset = Commands.Movies.UNSET.commandRegex()
    private val watchedSet = Commands.Movies.WATCHED_SET.commandRegex()
    private val watchedUnset = Commands.Movies.WATCHED_UNSET.commandRegex()
    private val roll = Commands.Movies.ROLL.commandRegex(singleWordCommand = false)
    private val search = Commands.Movies.SEARCH.commandRegex(singleWordCommand = false)
    private val roleSet = Commands.Movies.ROLE_SET.commandRegex(singleWordCommand = false)
    private val roleUnset = Commands.Movies.ROLE_UNSET.commandRegex(singleWordCommand = false)
    private val notifySet = Commands.Movies.NOTIFY_SET.commandRegex()
    private val notifyUnset = Commands.Movies.NOTIFY_UNSET.commandRegex()

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        val guildId = event.message.getGuild().id.value
        val channelId = event.message.channelId.value
        when {
            trimmedMessage.matches(top) -> {
                val moviesChannelId = guildsManager.getMoviesListChannel(guildId)
                if (moviesChannelId == null) {
                    postErrorMessage(channelId, "Не установлен канал со списком фильмов (${getCommand(Commands.Movies.SET)} в канале с фильмами)")
                } else {
                    showMoviesToRoll(channelId, moviesChannelId)
                }
            }
            trimmedMessage.matches(help) -> {
                showHelp(event.message.channel, guildsManager.getMoviesListChannel(guildId))
            }
            trimmedMessage.matches(set) -> {
                val changed = guildsManager.setMoviesListChannel(guildId, channelId)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(unset) -> {
                val changed = guildsManager.setMoviesListChannel(guildId, null)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(watchedSet) -> {
                val changed = guildsManager.setWatchedMoviesListChannel(guildId, channelId)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(watchedUnset) -> {
                val changed = guildsManager.setWatchedMoviesListChannel(guildId, null)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(roll) -> {
                val moviesListChannelId = guildsManager.getMoviesListChannel(guildId) ?: return
                rollMovies(event.message.channel, moviesListChannelId, trimmedMessage.contains("-s"))
            }
            trimmedMessage.matches(search) -> {
                searchForMovie(event.message.channel, trimmedMessage.removePrefix("search").trim())
            }
            trimmedMessage.matches(notifySet) -> {
                val changed = guildsManager.setMoviesNotifyChannel(guildId, channelId)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(notifyUnset) -> {
                val changed = guildsManager.setMoviesNotifyChannel(guildId, null)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(roleSet) -> {
                val roleId = trimmedMessage
                    .removePrefix(Commands.Movies.ROLE_SET)
                    .trim()
                    .let {
                        val regex = Regex("<@&(\\d+)>")
                        if (it.matches(regex)) {
                            it.replace(regex, "$1")
                        } else null
                    } ?: run {
                    respondWithReaction(event.message, false)
                    return
                }
                val changed = guildsManager.setMoviesRoleId(guildId, roleId)
                respondWithReaction(event.message, changed)
            }
            trimmedMessage.matches(roleUnset) -> {
                val changed = guildsManager.setMoviesRoleId(guildId, null)
                respondWithReaction(event.message, changed)
            }
        }
    }

    override suspend fun onReactionAdd(event: ReactionAddEvent) {
        val guildId = event.guildId?.value ?: ""
        val channelId = event.message.channelId.value
        when {
            event.emoji.name == "✅" && guildsManager.getMoviesListChannel(guildId) == channelId -> {
                client.setDefaultStatus()
                val watchedChannelId = guildsManager.getWatchedMoviesChannelId(guildId) ?: return
                val movieName = event.message.asMessage().content
                event.message.delete()
                client.rest.channel.createMessage(watchedChannelId) {
                    content = movieName
                }
            }
            event.emoji.name == "\uD83D\uDC40" && guildsManager.getMoviesListChannel(guildId) == channelId -> {
                val movieName = event.message.asMessage().content
                guildsManager.getMoviesNotifyChannel(guildId)?.let { moviesNotifyChannelId ->
                    val moviesRoleId = guildsManager.getMoviesRoleId(guildId)
                    val notification = moviesRoleId?.let {
                        "<@&$it>, смотрим $movieName"
                    } ?: "Смотрим \"$movieName\""
                    postMessage(moviesNotifyChannelId, notification, messageColor = Color.BLUE)
                }
            }
        }
    }

    private suspend fun showHelp(channel: MessageChannelBehavior, moviesChannelId: String?) {
        val rulesTextStart = if (moviesChannelId != null) {
            "В выборку попадают первые 100 фильмов из канала ${getChannelMention(moviesChannelId)}."
        } else {
            "В выборку попадают первые 100 фильмов из неустановленного канала (${getCommand("set")}, чтобы установить)."
        }

        channel.createEmbed {
            color = Color(90, 141, 62)
            field {
                name = "Фильмы"
                value = "Следующие команды отвечают за выбор, хранение и поиск фильмов ботом. Боту можно указать два канала: канал для предложенных фильмов и канал для просмотренных фильмов (подробнее в описаниях команд). Пометить фильм как просмотренный можно, выставив ему специальную реакцию (✅)."
            }
            field {
                name = "Правила выбора фильмов"
                value = "$rulesTextStart Фильм выбирается по наибольшему количеству лайков ( :thumbsup: ) в реакциях."
            }
            field {
                name = getCommand(Commands.Movies.SET)
                value = "Устанавливает канал, в который была отправлена команда, как канал откуда бот будет брать список фильмов."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.UNSET)
                value = "Отменяет предыдущую команду."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.WATCHED_SET)
                value = "Устанавливает канал, в который была отпрвлена команда, как канал куда бот будет сохранять список просмотренных каналов."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.WATCHED_UNSET)
                value = "Отменяет предыдущую команду."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.TOP)
                value = "Показывает список фильмов с максимальным количеством лайков."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.SEARCH)
                value = "Ищет фильм на Кинопоиске. Пример использования: `${getCommand(Commands.Movies.SEARCH, format = false)} Геи-ниггеры из далёкого космоса`."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.ROLL)
                value = "Выбирает случайный фильм из выборки, которую можно посмотреть по комманде ${getCommand(Commands.Movies.TOP)}. Если ввести параметр `-s`, бот найдёт выбранный фильм на Кинопоиске."
                inline = true
            }
            field {
                val inMoviesChannel = if (moviesChannelId != null) {
                    "Поставив реакцию :eyes: под выбранным фильмом в канале ${getChannelMention(moviesChannelId)}"
                } else {
                    "Поставив реакцию :eyes: под выбранным фильмом в неустановленном канале для фильмов"
                }
                name = "Уведомления для киноманов"
                value = "$inMoviesChannel, можно для установленной роли сделать уведомление о начале просмотра фильма."
            }
            field {
                name = getCommand(Commands.Movies.ROLE_SET)
                value = "Устанавливает роль, которая будет получать уведомления о начале просмотра фильма."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.ROLE_UNSET)
                value = "Сбрасывает роль для уведомлений."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.NOTIFY_SET)
                value = "Устанавливает текущий канал, как канал, куда будут присылаться уведомления."
                inline = true
            }
            field {
                name = getCommand(Commands.Movies.NOTIFY_UNSET)
                value = "Отменяет предыдущую команду."
                inline = true
            }
        }
    }

    private fun String.convertDate(): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val date = inputFormatter.parse(this)
            val outputFormatter = SimpleDateFormat("d LLLL yyyy", Locale.forLanguageTag("ru"))
            outputFormatter.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            this
        }
    }
}