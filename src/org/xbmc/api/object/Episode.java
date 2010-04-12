/*
 *      Copyright (C) 2005-2010 Team XBMC
 *      http://xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC Remote; see the file license.  If not, write to
 *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */

package org.xbmc.api.object;

public class Episode {

	public int id;
	public String title;
	public String plot;
	public double rating = 0.0;
	public String writer;
	public String firstAired;
	public boolean watched;
	public String director;
	public int season;
	public int episode;
	
	public Episode(int id, String title, String plot, double rating, String writer, String firstAired,
			boolean watched, String director, int season, int episode){
		this.id = id;
		this.title = title;
		this.plot = plot;
		this.rating = rating;
		this.writer = writer;
		this.firstAired = firstAired;
		this.watched = watched;
		this.director = director;
		this.season = season;
		this.episode = episode;
	}
}